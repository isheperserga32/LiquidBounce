<script lang="ts">
  import ToolTip from "../ToolTip.svelte";
  import {getSession, openScreen} from "../../../../integration/rest";
  import {onMount} from "svelte";
  import {listen} from "../../../../integration/ws";
  import {location} from "svelte-spa-router";

  let username = "";
  let avatar = "";
  let premium = true;

  const inAccountManager = $location === "/altmanager";
  
  async function refreshSession() {
      const session = await getSession();
      username = session.username;
      avatar = session.avatar;
      premium = session.premium;
  }

  onMount(async () => {
      await refreshSession();
  });

  listen("session", async () => {
      await refreshSession();
  });
</script>

<div class="account-wrapper">
  <div class="account-card">
      <div class="avatar-section">
          <div class="avatar-container">
              <object data={avatar} type="image/png" class="avatar" aria-label="avatar">
                  <img src="img/steve.png" alt="avatar" class="avatar">
              </object>
              <div class="status-indicator" class:premium></div>
          </div>
      </div>

      <div class="info-section">
          <div class="username-container">
              <span class="username">{username}</span>
              <span class="account-badge">
                  {#if premium}
                      <img src="img/menu/icon-crown.svg" alt="premium" class="badge-icon">
                      Premium
                  {:else}
                      <img src="img/menu/icon-offline.svg" alt="offline" class="badge-icon">
                      Offline
                  {/if}
              </span>
          </div>
      </div>

      <button 
          class="switch-account-btn" 
          disabled={inAccountManager} 
          type="button" 
          on:click={() => openScreen("altmanager")}
      >
          <ToolTip text="Switch Account"/>
          <div class="btn-content">
              <img class="icon" src="img/menu/icon-pen.svg" alt="switch account">
              <span class="btn-text">Switch</span>
          </div>
      </button>
  </div>
</div>

<style lang="scss">
  @import "../../../../colors";

  .account-wrapper {
      padding: 8px;
      background: linear-gradient(135deg, rgba($accent-color, 0.2), rgba($accent-color, 0.05));
      border-radius: 12px;
      backdrop-filter: blur(10px);
  }

  .account-card {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 12px;
      background: rgba($hotbar-base-color, 0.85);
      border-radius: 8px;
      width: 420px;
  }

  .avatar-section {
      position: relative;
  }

  .avatar-container {
      position: relative;
      width: 56px;
      height: 56px;
  }

  .avatar {
      width: 100%;
      height: 100%;
      border-radius: 12px;
      object-fit: cover;
      border: 2px solid rgba($menu-text-color, 0.1);
  }

  .status-indicator {
      position: absolute;
      bottom: -4px;
      right: -4px;
      width: 14px;
      height: 14px;
      border-radius: 50%;
      border: 2px solid rgba($hotbar-base-color, 0.85);
      background-color: $menu-text-dimmed-color;

      &.premium {
          background-color: $menu-account-premium-color;
      }
  }

  .info-section {
      flex: 1;
  }

  .username-container {
      display: flex;
      flex-direction: column;
      gap: 4px;
  }

  .username {
      color: $menu-text-color;
      font-size: 18px;
      font-weight: 600;
      line-height: 1.2;
  }

  .account-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 500;
      color: $menu-text-dimmed-color;

      .badge-icon {
          width: 14px;
          height: 14px;
          opacity: 0.8;
      }
  }

  .switch-account-btn {
      background: rgba($accent-color, 0.1);
      border: none;
      border-radius: 8px;
      padding: 10px 16px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
          background: rgba($accent-color, 0.2);
      }

      &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
      }

      .btn-content {
          display: flex;
          align-items: center;
          gap: 8px;
      }

      .icon {
          width: 16px;
          height: 16px;
      }

      .btn-text {
          color: $menu-text-color;
          font-size: 14px;
          font-weight: 500;
      }
  }
</style>