<script lang="ts">
    import MainButton from "./buttons/MainButton.svelte";
    import ChildButton from "./buttons/ChildButton.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import {
        exitClient,
        openScreen,
        toggleBackgroundShaderEnabled
    } from "../../../integration/rest";
    import Menu from "../common/Menu.svelte";
</script>

<Menu>
    <div class="menu-wrapper">
        <div class="logo-section">
            <img class="main-logo" src="img/splash.png" alt="logo"/>
        </div>

        <div class="navigation-section">
            <div class="button-grid">
                <div class="left-buttons">
                    <MainButton title="Singleplayer" icon="singleplayer"
                              on:click={() => openScreen("singleplayer")}/>
                    <MainButton title="Options" icon="clickgui"
                              on:click={() => openScreen("clickgui")}/>
                </div>
                
                <div class="right-buttons">
                    <MainButton title="Multiplayer" icon="multiplayer"
                              on:click={() => openScreen("multiplayer")}>
                        <!-- <ChildButton title="Realms" icon="realms" parentHovered={false} -->
                                   <!-- on:click={() => openScreen("multiplayer_realms")}/> -->
                    </MainButton>
                    <MainButton title="Options" icon="options"
                              on:click={() => openScreen("options")}/>
                </div>
            </div>
        </div>

        <div class="bottom-section">
            <ButtonContainer>
                <IconTextButton icon="icon-exit.svg" title="Exit" on:click={exitClient}/>
                <IconTextButton icon="icon-change-background.svg" title="Toggle Shader"
                              on:click={toggleBackgroundShaderEnabled}/>
            </ButtonContainer>
        </div>
    </div>
</Menu>

<style lang="scss">
    @import "../../../colors.scss";

    .menu-wrapper {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        align-items: center;
        padding: 40px 0;
    }

    .logo-section {
        text-align: center;
        margin-top: 40px;

        .main-logo {
            width: 320px;
            height: auto;
            filter: drop-shadow(0 0 25px rgba(0, 0, 0, 0.4));
            margin-bottom: 20px;
            transition: transform 0.3s ease;

            &:hover {
                transform: scale(1.02);
            }
        }

        .version-info {
            background: linear-gradient(135deg, rgba($menu-base-color, 0.9), rgba($menu-base-color, 0.7));
            padding: 15px 40px;
            border-radius: 12px;
            backdrop-filter: blur(10px);
            display: inline-block;

            .version {
                display: block;
                color: $menu-text-color;
                font-size: 32px;
                font-weight: 600;
                margin-bottom: 5px;
            }

            .build {
                color: $menu-text-color;
                font-size: 18px;
                opacity: 0.8;
            }
        }
    }

    .navigation-section {
        margin: 60px 0;
        width: 100%;
        max-width: 1200px;
    }

    .button-grid {
        display: flex;
        justify-content: center;
        gap: 40px;

        .left-buttons, .right-buttons {
            display: flex;
            flex-direction: column;
            gap: 20px;
            width: 400px;
        }
    }

    .bottom-section {
        display: flex;
        justify-content: center;
        width: 100%;
        padding: 0 20px;
    }
</style>