import type {TransitionConfig} from "svelte/transition";

interface ExtendedTransitionConfig extends Partial<TransitionConfig> {
    fn: Function;
    animate: boolean;
    [x: string]: unknown;
}

export function maybe(node: HTMLElement, options: ExtendedTransitionConfig) {
    if (options.animate) {
        return options.fn(node, options);
    }
}