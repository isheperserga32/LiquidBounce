/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.block

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.getValue
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.chunk.WorldChunk
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException

object ChunkScanner : Listenable {
    private val subscribers = CopyOnWriteArrayList<BlockChangeSubscriber>()

    private val loadedChunks = hashSetOf<ChunkLocation>()

    private fun clearAllChunks() {
        subscribers.forEach(BlockChangeSubscriber::clearAllChunks)
        loadedChunks.clear()
    }

    @Suppress("unused")
    val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        val chunk = mc.world!!.getChunk(event.x, event.z)

        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(chunk))

        this.loadedChunks.add(ChunkLocation(event.x, event.z))
    }

    @Suppress("unused")
    val chunkDeltaUpdateHandler = handler<ChunkDeltaUpdateEvent> { event ->
        val chunk = mc.world!!.getChunk(event.x, event.z)
        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(chunk))
    }

    @Suppress("unused")
    val chunkUnloadHandler = handler<ChunkUnloadEvent> { event ->
        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUnloadRequest(event.x, event.z))

        this.loadedChunks.remove(ChunkLocation(event.x, event.z))
    }

    @Suppress("unused")
    val blockChangeEvent = handler<BlockChangeEvent> { event ->
        ChunkScannerThread.enqueueChunkUpdate(
            ChunkScannerThread.UpdateRequest.BlockUpdateEvent(
                event.blockPos,
                event.newState
            )
        )
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> { event ->
        clearAllChunks()
    }

    @Suppress("unused")
    val disconnectHandler = handler<DisconnectEvent> {
        clearAllChunks()
    }

    fun subscribe(newSubscriber: BlockChangeSubscriber) {
        check(newSubscriber !in this.subscribers) {
            "Subscriber ${newSubscriber.javaClass.simpleName} already registered"
        }

        subscribers.add(newSubscriber)

        val world = mc.world ?: return

        logger.debug("Scanning ${this.loadedChunks.size} chunks for ${newSubscriber.javaClass.simpleName}")

        for (loadedChunk in this.loadedChunks) {
            ChunkScannerThread.enqueueChunkUpdate(
                ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(
                    world.getChunk(
                        loadedChunk.x,
                        loadedChunk.z
                    ),
                    newSubscriber
                )
            )
        }
    }

    fun unsubscribe(oldSubscriber: BlockChangeSubscriber) {
        subscribers.remove(oldSubscriber)
        oldSubscriber.clearAllChunks()
    }

    object ChunkScannerThread {
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        /**
         * Shared cache for CoroutineScope
         */
        private val mutable by ThreadLocal.withInitial(BlockPos::Mutable)

        private const val CHANNEL_CAPACITY = 800

        private var chunkUpdateChannel = Channel<UpdateRequest>(capacity = CHANNEL_CAPACITY)

        private val channelRestartMutex = Mutex()

        init {
            // cyclic job, used to process tasks from channel
            @Suppress("detekt:SwallowedException")
            scope.launch {
                var retrying = 0
                while (true) {
                    try {
                        val chunkUpdate = chunkUpdateChannel.receive()

                        if (mc.world == null) {
                            // reset Channel (prevent sending)
                            channelRestartMutex.withLock {
                                chunkUpdateChannel.cancel()
                                chunkUpdateChannel = Channel(capacity = CHANNEL_CAPACITY)
                            }
                            // max delay = 30s (1s, 2s, 4s, ...)
                            delay((1000L shl retrying++).coerceAtMost(30000L))
                            continue
                        }

                        retrying = 0

                        // process the update request
                        // TODO: may need to start a new job
                        when (chunkUpdate) {
                            is UpdateRequest.ChunkUpdateRequest -> scanChunk(chunkUpdate)

                            is UpdateRequest.ChunkUnloadRequest -> subscribers.forEach {
                                it.clearChunk(chunkUpdate.x, chunkUpdate.z)
                            }

                            is UpdateRequest.BlockUpdateEvent -> subscribers.forEach {
                                it.recordBlock(chunkUpdate.blockPos, chunkUpdate.newState, cleared = false)
                            }
                        }
                    } catch (e: CancellationException) {
                        break // end loop if job has been canceled
                    } catch (e: ClosedReceiveChannelException) {
                        break // the channel is closed from outside (stopThread)
                    } catch (e: Throwable) {
                        retrying++
                        logger.warn("Chunk update error", e)
                    }
                }
            }
        }

        fun enqueueChunkUpdate(request: UpdateRequest) {
            scope.launch {
                channelRestartMutex.withLock {
                    chunkUpdateChannel.send(request)
                }
            }
        }

        /**
         * Scans the chunks for a block
         */
        private suspend fun scanChunk(request: UpdateRequest.ChunkUpdateRequest) {
            val chunk = request.chunk

            if (chunk.isEmpty) {
                return
            }

            val currentSubscriber = request.singleSubscriber?.let { listOf(it) } ?: subscribers

            currentSubscriber.map {
                scope.launch {
                    it.chunkUpdate(request.chunk.pos.x, request.chunk.pos.z)
                }
            }.joinAll()

            // Contains all subscriber that want recordBlock called on a chunk update
            val subscribersForRecordBlock = currentSubscriber.filter { it.shouldCallRecordBlockOnChunkUpdate }

            if (subscribersForRecordBlock.isEmpty()) {
                return
            }

            val start = System.nanoTime()

            (chunk.bottomY..chunk.topY).map { y ->
                scope.launch {
                    val startX = chunk.pos.startX
                    val startZ = chunk.pos.startZ
                    for (x in 0..15) {
                        for (z in 0..15) {
                            val pos = mutable.set(startX or x, y, startZ or z)
                            val blockState = chunk.getBlockState(pos)
                            subscribersForRecordBlock.forEach { it.recordBlock(pos, blockState, cleared = true) }
                        }
                    }
                }
            }.joinAll()

            logger.debug("Scanning chunk (${chunk.pos.x}, ${chunk.pos.z}) took ${(System.nanoTime() - start) / 1000}us")
        }

        fun stopThread() {
            scope.cancel()
            chunkUpdateChannel.close()
            logger.info("Stopped Chunk Scanner Thread!")
        }

        sealed interface UpdateRequest {
            @JvmRecord
            data class ChunkUpdateRequest(val chunk: WorldChunk, val singleSubscriber: BlockChangeSubscriber? = null) :
                UpdateRequest

            @JvmRecord
            data class ChunkUnloadRequest(val x: Int, val z: Int) : UpdateRequest

            @JvmRecord
            data class BlockUpdateEvent(val blockPos: BlockPos, val newState: BlockState) : UpdateRequest
        }
    }

    interface BlockChangeSubscriber {
        /**
         * If this is true [recordBlock] is called on chunk updates and on single block updates.
         * This might be inefficient for some modules, so they can choose to not call that method on chunk updates.
         */
        val shouldCallRecordBlockOnChunkUpdate: Boolean
            get() = true

        /**
         * Registers a block update and asks the subscriber to make a decision about what should be done.
         * This method must be **thread-safe**.
         *
         * @param pos DON'T directly save it to a container Property (Field in Java), save a copy instead
         * @param cleared true, if the section the block is in was already cleared
         */
        fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean)

        /**
         * Is called when a chunk is loaded or entirely updated.
         */
        fun chunkUpdate(x: Int, z: Int)
        fun clearChunk(x: Int, z: Int)
        fun clearAllChunks()
    }

    @JvmRecord
    data class ChunkLocation(val x: Int, val z: Int)
}
