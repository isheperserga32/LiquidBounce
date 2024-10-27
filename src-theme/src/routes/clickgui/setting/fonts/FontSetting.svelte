<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {ModuleSetting, TextSetting} from "../../../../integration/types";
    import {getFonts} from "../../../../integration/rest";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import Font from "./Font.svelte";
    import VirtualList from "../blocks/VirtualList.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as TextSetting;

    const dispatch = createEventDispatcher();
    let fonts: string[] = [];
    let shownFonts: string[] = fonts;
    let searchQuery = "";

    $: {
        let filteredFonts = fonts;
        if (searchQuery) {
            filteredFonts = filteredFonts.filter(b => b.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        shownFonts = filteredFonts;
    }

    onMount(async () => {
        fonts = (await getFonts()).sort((a, b) => a.localeCompare(b));
    });

    function handleFontSelect(e: CustomEvent<{ name: string }>) {
        const selectedFont = e.detail.name;
        setting = { ...cSetting, value: selectedFont };
        dispatch("change", { setting });
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
    <div class="results">
        <VirtualList items={shownFonts} let:item>
            <Font name={item} highlight={cSetting.value === item} on:click={handleFontSelect}/>
        </VirtualList>
    </div>
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .setting {
    padding: 7px 0;
  }

  .results {
    height: 100px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .name {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 500;
    margin-bottom: 5px;
  }

  .search-input {
    width: 100%;
    border: none;
    border-bottom: solid 1px $accent-color;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    padding: 5px;
    color: $clickgui-text-color;
    margin-bottom: 5px;
    background-color: rgba($clickgui-base-color, .36);
  }
</style>
