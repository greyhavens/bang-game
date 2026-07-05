//
// $Id$

package com.threerings.jme.model;

/**
 * The single definition of the per-model {@code bang.*} scene-graph user data that the build-time
 * model writers encode into each baked {@code .j3o} and that the runtime {@link Model} facade
 * decodes. Both sides must agree on these key names, the loop-mode vocabulary, and the frame-rate
 * default, so they live here once (Epic 4 Phase 2b) rather than as literals duplicated across every
 * producer and consumer — notably so the Phase-2b-ii glTF writer reuses the contract instead of
 * adding a fourth copy.
 *
 * <p>The Phase-5-doomed oracle {@code ModelConverter} (tools/j3o-converter) keeps its own literals
 * on purpose: the parity harness diffing the native bake against that oracle is exactly the safety
 * net that would catch any accidental drift introduced here, and it is deleted with the fork.
 */
public final class ModelMetadata
{
    /** Root user data: the {@code model.properties} text ({@code key = value} lines). */
    public static final String PROPERTIES = "bang.properties";

    /** Root user data: comma-separated animation (clip) names. */
    public static final String ANIMATIONS = "bang.animations";

    /** Root user data: the model type path (rsrc-relative directory). */
    public static final String TYPE = "bang.type";

    /** Root user data: comma-separated {@code name=LoopMode} pairs (values from {@link #loopMode}). */
    public static final String LOOP_MODES = "bang.loopModes";

    /** Root user data: comma-separated {@code name=fps} pairs. */
    public static final String FRAME_RATES = "bang.frameRates";

    /** Geometry user data: comma-separated candidate texture names. */
    public static final String TEXTURES = "bang.textures";

    /** Frame rate assumed for an animation absent from {@link #FRAME_RATES}. */
    public static final int DEFAULT_FRAME_RATE = 30;

    /** {@link #LOOP_MODES} value marking a one-shot (non-looping) animation; any other value loops. */
    public static final String LOOP_NONE = "DontLoop";

    /** Alpha-discard threshold for a transparent material with no explicit {@code alpha} property. */
    public static final float DEFAULT_ALPHA_THRESHOLD = 0.5f;

    /**
     * Maps a fork {@code repeat_type} property value to the jME3 loop-mode name stored in
     * {@link #LOOP_MODES} (fork {@code JmeUtil.parseRepeatType} + {@code ModelConverter.loopMode};
     * the fork default RT_CLAMP is a one-shot).
     */
    public static String loopMode (String repeatType)
    {
        if ("wrap".equals(repeatType)) {
            return "Loop";
        } else if ("cycle".equals(repeatType)) {
            return "Cycle";
        }
        return LOOP_NONE;
    }

    private ModelMetadata () {}
}
