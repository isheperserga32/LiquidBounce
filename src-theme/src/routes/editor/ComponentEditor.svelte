<script lang="ts">
    import {onMount} from "svelte";
    import {deleteComponent, getComponent, updateComponent,} from "../../integration/rest";
    import type {ConfigurableSetting} from "../../integration/types";
    import GenericSetting from "../clickgui/setting/common/GenericSetting.svelte";

    export let name: string;
    export let id: string;

    // todo: what is this?
    let path = `editor.${id}`;

    let configurable: ConfigurableSetting;

    onMount(async () => {
        configurable = await getComponent(id);
    });

    async function updateComponentSettings() {
        await updateComponent(id, configurable);
        configurable = await getComponent(id);

    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="component">
    {name}
    <button on:click={() => deleteComponent(id)}>-</button>

    {#if configurable}
        <div class="settings">
            {#each configurable.value as setting (setting.name)}
                {#if setting.valueType}
                    <GenericSetting skipAnimationDelay={true} {path} bind:setting on:change={updateComponentSettings}/>
                {/if}
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  button {
    background-color: #4677ff; /* Accent color */
    color: white;
    border: none;
    padding: 0.5rem 0.75rem;
    cursor: pointer;
    font-size: 0.8rem;
    margin-left: 15px;
    border-radius: 4px;
    transition: background-color 0.2s;
  }

  button:hover {
    background-color: #365bb8; /* Darker on hover */
  }
</style>
