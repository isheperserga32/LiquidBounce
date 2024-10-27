<script lang="ts">
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher();

    export let name: string;
    export let highlight: boolean;
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="font" on:click={() => dispatch("click", {name})}>
    <div class="name {highlight ? 'highlight' : ''}" style="font-family: {name};">{name}</div>
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
    font-size: 25px;
    color: $clickgui-text-color;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;

    &.highlight {
        color: $accent-color;
    }
  }
</style>
