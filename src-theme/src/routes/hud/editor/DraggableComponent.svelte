<script lang="ts">
    import {type Alignment, HorizontalAlignment, VerticalAlignment} from "../../../integration/types.js";
    import {scaleFactor} from "../../clickgui/clickgui_store";
    import {moveComponent} from "../../../integration/rest";
    import {onMount} from "svelte";
    import ComponentSettings from "./ComponentSettings.svelte";

    export let alignment: Alignment;
    export let id: string;
    export let name: string;
    export let editorMode: boolean;

    let element: HTMLElement | undefined;

    let moving = false;
    let prevX = 0;
    let prevY = 0;

    let settingsBottom = false;

    function updateSettingsBottom() {
        settingsBottom = (element?.offsetTop ?? 0) < window.innerHeight / 2 - (element?.offsetHeight ?? 0) / 2;
    }

    $: styleString = generateStyleString(alignment);

    function clamp(number: number, min: number, max: number) {
        return Math.max(min, Math.min(number, max));
    }

    function onMouseDown() {
        moving = true;
    }

    function onMouseMove(e: MouseEvent) {
        if (moving) {
            switch (alignment.horizontal) {
                case HorizontalAlignment.CENTER_TRANSLATED:
                case HorizontalAlignment.CENTER:
                case HorizontalAlignment.LEFT:
                    alignment.horizontalOffset += (e.screenX - prevX) * (2 / $scaleFactor);

                    break;
                case HorizontalAlignment.RIGHT:
                    alignment.horizontalOffset -= (e.screenX - prevX) * (2 / $scaleFactor);
                    break;
            }

            switch (alignment.horizontal) {
                case HorizontalAlignment.CENTER_TRANSLATED:
                    alignment.horizontalOffset = clamp(
                        alignment.horizontalOffset,
                        -window.innerWidth / 2 + (element?.offsetWidth ?? 0) / 2,
                        window.innerWidth / 2 - (element?.offsetWidth ?? 0) / 2
                    );
                    break;
                case HorizontalAlignment.CENTER:
                    alignment.horizontalOffset = clamp(
                        alignment.horizontalOffset,
                        -window.innerWidth / 2,
                        window.innerWidth / 2
                    );
                    break;
                case HorizontalAlignment.LEFT:
                case HorizontalAlignment.RIGHT:
                    alignment.horizontalOffset = clamp(alignment.horizontalOffset, 0, window.innerWidth - (element?.offsetWidth ?? 0));
                    break;
            }

            switch (alignment.vertical) {
                case VerticalAlignment.CENTER:
                case VerticalAlignment.CENTER_TRANSLATED:
                case VerticalAlignment.TOP:
                    alignment.verticalOffset += (e.screenY - prevY) * (2 / $scaleFactor);
                    break;
                case VerticalAlignment.BOTTOM:
                    alignment.verticalOffset -= (e.screenY - prevY) * (2 / $scaleFactor);
                    break;
            }

            switch (alignment.vertical) {
                case VerticalAlignment.CENTER_TRANSLATED:
                    alignment.verticalOffset = clamp(
                        alignment.verticalOffset,
                        -window.innerHeight / 2 + (element?.offsetHeight ?? 0) / 2,
                        window.innerHeight / 2 - (element?.offsetHeight ?? 0) / 2
                    );
                    break;
                case VerticalAlignment.CENTER:
                    alignment.verticalOffset = clamp(
                        alignment.verticalOffset,
                        -window.innerHeight / 2,
                        window.innerHeight / 2
                    );
                    break;
                case VerticalAlignment.TOP:
                case VerticalAlignment.BOTTOM:
                    alignment.verticalOffset = clamp(alignment.verticalOffset, 0, window.innerHeight - (element?.offsetHeight ?? 0));
                    break;
            }
        }

        prevX = e.screenX;
        prevY = e.screenY;

        updateSettingsBottom();
    }

    async function onMouseUp() {
        moving = false;
        await moveComponent(id, alignment);
    }

    function generateStyleString(alignment: Alignment): string {
        let style = "position: fixed;";

        switch (alignment.horizontal) {
            case HorizontalAlignment.LEFT:
                style += `left: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.RIGHT:
                style += `right: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.CENTER:
            case HorizontalAlignment.CENTER_TRANSLATED:
                style += `left: calc(50% + ${alignment.horizontalOffset}px);`;
                break;
        }

        switch (alignment.vertical) {
            case VerticalAlignment.TOP:
                style += `top: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.BOTTOM:
                style += `bottom: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.CENTER:
            case VerticalAlignment.CENTER_TRANSLATED:
                style += `top: calc(50% + ${alignment.verticalOffset}px);`;
                break;
        }

        style += "transform: translate("
        if (alignment.horizontal === HorizontalAlignment.CENTER_TRANSLATED) {
            style += "-50%,";
        } else {
            style += "0,";
        }
        if (alignment.vertical === VerticalAlignment.CENTER_TRANSLATED) {
            style += "-50%);";
        } else {
            style += "0);"
        }

        return style;
    }

    onMount(() => {
        updateSettingsBottom();
    });
</script>

<svelte:window on:mouseup={onMouseUp} on:mousemove={onMouseMove}/>

<div class="draggable-element" style={styleString} bind:this={element}>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="contained-element" on:mousedown={onMouseDown} class:editor-mode={editorMode}>
        <slot/>
    </div>
    {#if editorMode}
        <ComponentSettings {name} {id} bottom={settingsBottom}/>
    {/if}
</div>

<style lang="scss">
  @import "../../../colors";

  .draggable-element {
    position: relative;
  }

  .contained-element {
    min-width: 50px;
    min-height: 50px;
  }

  .editor-mode {
    border: solid 1px $hud-editor-element-border-color;
    background-color: $hud-editor-element-background-color;
  }
</style>