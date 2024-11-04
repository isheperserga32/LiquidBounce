<script lang="ts">
    import {type Alignment, HorizontalAlignment, VerticalAlignment} from "../../integration/types.js";
    import {scaleFactor} from "../clickgui/clickgui_store";
    import {moveComponent} from "../../integration/rest";

    export let alignment: Alignment;
    export let id: string;
    export let name: string;
    export let editorMode: boolean;

    let element: HTMLElement | undefined;

    let moving = false;
    let prevX = 0;
    let prevY = 0;

    let descriptorBottom = false;

    function updateDescriptorBottom() {
        descriptorBottom = (element?.offsetTop ?? 0) < window.innerHeight / 2 - (element?.offsetHeight ?? 0) / 2;
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

        updateDescriptorBottom();
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

    updateDescriptorBottom();
</script>

<svelte:window on:mouseup={onMouseUp} on:mousemove={onMouseMove}/>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="draggable-element" style={styleString} on:mousedown={onMouseDown} bind:this={element}
     class:editor-mode={editorMode}>
    <slot/>
    {#if editorMode}
        <div class="descriptor" class:bottom={descriptorBottom}>{name}</div>
    {/if}
</div>

<style lang="scss">
  @import "../../colors";

  .draggable-element {
    position: relative;
    min-width: 50px;
    min-height: 50px;
  }

  .editor-mode {
    border: solid 1px $hud-editor-element-border-color;
    background-color: rgba($hud-editor-element-background-color, 0.1);
  }

  .descriptor {
    background-color: $hud-editor-descriptor-background-color;
    color: $hud-editor-descriptor-color;
    padding: 5px 8px;
    border-radius: 5px;
    position: absolute;
    top: -45px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 14px;

    &::after {
      content: "";
      display: block;
      position: absolute;
      width: 0;
      height: 0;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-right: 8px solid $hud-editor-descriptor-background-color;
      left: 50%;
      bottom: -12px;
      transform: translateX(-50%) rotate(-90deg);
    }

    &.bottom {
      top: unset;
      bottom: -45px;

      &::after {
        top: -12px;
        transform: translateX(-50%) rotate(90deg);
      }
    }
  }
</style>