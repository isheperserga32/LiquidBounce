<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {REST_BASE} from "../../../../integration/host";

    const dispatch = createEventDispatcher();

    export let name: string;
    export let highlight: boolean;

    // Dynamic @font-face injection
    const fontFace = new FontFace(name, `url('${REST_BASE}/api/v1/client/fonts/${name}')`);
    fontFace.load().then((loadedFont) => {
        document.fonts.add(loadedFont);
    }).catch((error) => {
        console.error("Failed to load font:", error);
    });
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="font" on:click={() => dispatch("click", {name})}>
    <div class="name" class:highlight={highlight} style="font-family: {name};">
        {name}
    </div>
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .font {
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    column-gap: 5px;
    cursor: pointer;
    margin: 2px 5px 2px 0;
  }

  .name {
    font-size: 15px;
    color: $clickgui-text-color;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;

    &.highlight {
      color: $accent-color;
    }
  }
</style>
