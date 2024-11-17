<script lang="ts">
    import Account from "./Account.svelte";
    import Notifications from "./Notifications.svelte";
</script>

<div class="header">
    <!-- <img class="logo" src="img/splash.png" height="98" width="98" alt="logo"> -->

    <Notifications />

    <Account/>
</div>

<style lang="scss">
  .header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 60px;
    align-items: center;
  }
</style>