<script lang="ts">
  import {fade} from "svelte/transition";
  import {createEventDispatcher} from "svelte";

  export let title: string;
  export let icon: string;

  let hovered = false;
  const dispatch = createEventDispatcher();
</script>

<div class="main-button" 
   on:mouseenter={() => hovered = true}
   on:mouseleave={() => hovered = false}
   on:click={() => dispatch("click")}>
  
  <div class="button-content">
      <div class="icon-wrapper">
          <div class="icon">
              {#if !hovered}
                  <img  
                       src="img/menu/icon-{icon}.svg" alt={icon}>
              {:else}
                  <img  
                       src="img/menu/icon-{icon}-hover.svg" alt={icon}>
              {/if}
          </div>
      </div>

      <span class="title">{title}</span>

      <div class="child-wrapper" class:show={hovered}>
          <slot parentHovered={hovered}/>
      </div>
  </div>

  <div class="hover-indicator" class:show={hovered}></div>
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .main-button {
      position: relative;
      background-color: rgba($menu-base-color, 0.75);
      border-radius: 8px;
      overflow: visible;
      cursor: pointer;
      transition: all 0.3s ease;
      backdrop-filter: blur(8px);

      &:hover {
          background-color: rgba($menu-base-color, 0.85);
          transform: translateX(15px);

          .icon {
              background-color: $menu-text-color;
          }
      }
  }

  .button-content {
      position: relative;
      display: flex;
      align-items: center;
      padding: 12px 25px;
      gap: 25px;
      z-index: 2;
  }

  .icon-wrapper {
      position: relative;
      
      &::after {
          content: '';
          position: absolute;
          top: -2px;
          left: -2px;
          right: -2px;
          bottom: -2px;
          background: linear-gradient(135deg, $accent-color, rgba($accent-color, 0.5));
          border-radius: 12px;
          opacity: 0;
          transition: opacity 0.3s ease;
      }
  }

  .icon {
      width: 52px;
      height: 52px;
      background-color: $accent-color;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;
      position: relative;
      z-index: 1;

      img {
          width: 26px;
          height: 26px;
          transition: transform 0.3s ease;
      }
  }

  .title {
      color: $menu-text-color;
      font-size: 22px;
      font-weight: 600;
      transition: transform 0.3s ease;
      flex: 1;
  }

  .child-wrapper {
      position: absolute;
      right: 25px;
      opacity: 0;
      transform: translateX(-15px);
      transition: all 0.3s ease;
      pointer-events: none;

      &.show {
          opacity: 1;
          transform: translateX(0);
          pointer-events: all;
      }
  }

  .hover-indicator {
      position: absolute;
      left: 0;
      top: 0;
      height: 100%;
      width: 4px;
      background: $accent-color;
      opacity: 0;
      transition: opacity 0.3s ease;

      &.show {
          opacity: 1;
      }
  }
</style>