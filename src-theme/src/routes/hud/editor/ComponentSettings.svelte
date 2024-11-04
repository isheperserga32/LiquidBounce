<script lang="ts">
    import type {ConfigurableSetting} from "../../../integration/types";
    import {onMount} from "svelte";
    import {
        getComponentSettings,
        setComponentSettings
    } from "../../../integration/rest";
    import GenericSetting from "../../clickgui/setting/common/GenericSetting.svelte";

    export let name: string;
    export let id: string;
    export let bottom: boolean;

    let configurable: ConfigurableSetting | undefined;

    async function handleSettingChange() {
        await setComponentSettings(id, configurable!!);
        configurable = await getComponentSettings(id);
    }

    onMount(async () => {
        configurable = await getComponentSettings(id);
    });
</script>

<div class="settings" class:bottom={bottom}>
    {#if configurable !== undefined}
        <GenericSetting path={name} bind:setting={configurable} on:change={handleSettingChange}/>
    {/if}
</div>

<style lang="scss">
  @import "../../../colors";

  .settings {
    background-color: $hud-editor-descriptor-background-color;
    padding: 5px 10px;
    border-radius: 5px;
    position: absolute;
    top: -15px;
    left: 50%;
    transform: translateY(-100%) translateX(-50%);
    width: 200px;
    box-shadow: $hud-editor-descriptor-shadow;

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
      bottom: -15px;
      transform: translateY(100%) translateX(-50%);;

      &::after {
        top: -12px;
        transform: translateX(-50%) rotate(90deg);
      }
    }
  }
</style>