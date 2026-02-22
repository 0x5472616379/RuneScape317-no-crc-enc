// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.io.IOException;

public class SeqType {

    public static int count;
    public static SeqType[] instances;

    public static void unpack(FileArchive archive) throws IOException {
        Buffer buffer = new Buffer(archive.read("seq.dat"));
        count = buffer.readU16();
        if (instances == null) {
            instances = new SeqType[count];
        }

        for (int i = 0; i < count; i++) {
            if (instances[i] == null) {
                instances[i] = new SeqType();
            }
            instances[i].load(buffer);
        }
    }

    /**
     * The amount of frames in this {@link SeqType}.
     */
    public int frameCount;

    /**
     * The list of {@link SeqTransform} indices indexed by Frame ID.
     *
     * @see SeqTransform
     */
    public int[] transformIDs;

    /**
     * Auxiliary transform indices appear to only be used by a {@link IfType} of type <code>6</code> as seen in {@link Game#drawParentInterface(IfType, int, int, int)}.
     */
    public int[] auxiliaryTransformIDs;

    /**
     * A list of durations indexed by Frame ID.
     */
    public int[] frameDuration;

    /**
     * The number of frames from the end of this {@link SeqType} used for looping.
     */
    public int loopFrameCount = -1;

    /**
     * Used to determine which transform bases a primary frame is allowed to use, and secondary not.
     *
     * @see Model#applyTransforms(int, int, int[])
     */
    public int[] mask;

    /**
     * Adds additional space to the render bounds.
     *
     * @see Scene#addTemporary(Entity, int, int, int, int, int, int, boolean, int)
     */
    public boolean forwardRenderPadding = false;

    /**
     * The priority.
     */
    public int priority = 5;

    /**
     * Allows this {@link SeqType} to override the right hand of a {@link PlayerEntity}.
     *
     * @see PlayerEntity#getSequencedModel()
     */
    public int rightHandOverride = -1;

    /**
     * Allows this {@link SeqType} to override the left hand of a {@link PlayerEntity}.
     *
     * @see PlayerEntity#getSequencedModel()
     */
    public int leftHandOverride = -1;

    /**
     * How many times this seq is allowed to loop before stopping.
     */
    public int loopCount = 99;

    /**
     * If 0, causes faster movement, allows looking at target
     * If 1, pause while moving
     * If 2, does not look at target, continues playing during movement
     *
     * @see Game#updateMovement(PathingEntity)
     */
    public int moveStyle = -1;

    /**
     * If 0, allows looking at a target
     * If 1, stops playing on move
     * If 2, does not look at target, continues playing during movement
     *
     * @see Game#updateMovement(PathingEntity)
     */
    public int idleStyle = -1;

    /**
     * If 1, restarts the sequence if already playing
     * If 2, does not restart if already playing
     *
     * @see Game#readNPCUpdates()
     */
    public int replayStyle = 1;

    public SeqType() {
    }

    public int getFrameDuration(int frame) {
        int duration = frameDuration[frame];

        if (duration == 0) {
            SeqTransform transform = SeqTransform.get(transformIDs[frame]);
            if (transform != null) {
                duration = frameDuration[frame] = transform.delay;
            }
        }

        if (duration == 0) {
            duration = 1;
        }

        return duration;
    }

    public void load(Buffer buffer) {
        while (true) {
            int code = buffer.readU8();

            if (code == 0) {
                break;
            } else if (code == 1) {
                frameCount = buffer.readU16();
                transformIDs = new int[frameCount];
                auxiliaryTransformIDs = new int[frameCount];
                frameDuration = new int[frameCount];

                for (int f = 0; f < frameCount; f++) {
                    frameDuration[f] = buffer.readU16();
                }
                for (int f = 0; f < frameCount; f++) {
                    transformIDs[f] = buffer.readU16();
                    auxiliaryTransformIDs[f] = -1;
                }
                for (int f = 0; f < frameCount; f++) {
                    transformIDs[f] += buffer.readU16() << 16;
                }

            } else if (code == 2) {
                loopFrameCount = buffer.readU16();
            } else if (code == 3) {
                int count = buffer.readU8();
                mask = new int[count + 1];
                for (int l = 0; l < count; l++) {
                    mask[l] = buffer.readU8();
                }
                mask[count] = 9999999;
            } else if (code == 4) {
                forwardRenderPadding = true;
            } else if (code == 5) {
                priority = buffer.readU8();
            } else if (code == 6) {
                rightHandOverride = buffer.readU16();
            } else if (code == 7) {
                leftHandOverride = buffer.readU16();
            } else if (code == 8) {
                loopCount = buffer.readU8();
            } else if (code == 9) {
                moveStyle = buffer.readU8();
            } else if (code == 10) {
                idleStyle = buffer.readU8();
            } else if (code == 11) {
                replayStyle = buffer.readU8();
            } else if (code == 12) {
                int len = buffer.readU8();
                for (int i = 0; i < len; i++) buffer.readU16();
                for (int i = 0; i < len; i++) buffer.readU16();
            } else if (code == 13) {
                int len = buffer.readU8();
                for (int i = 0; i < len; i++) buffer.read24();
            } else if (code == 14) {
                buffer.read32();
            } else if (code == 15) {
                int count = buffer.readU16();
                for (int i = 0; i < count; i++) {
                    buffer.readU16();
                    buffer.read24();
                }
            } else if (code == 16) {
                buffer.readU16();
                buffer.readU16();
            } else if (code == 17) {
                int count = buffer.readU8();
                for (int i = 0; i < count; i++) buffer.readU8();
            } else if (code == 18 || code == 127) {
                // no payload
            } else {
                throw new IllegalStateException("Unrecognised seq config code: " + code);
            }
        }

        if (frameCount == 0) {
            frameCount = 1;
            transformIDs = new int[] { -1 };
            auxiliaryTransformIDs = new int[] { -1 };
            frameDuration = new int[] { -1 };
        }

        if (moveStyle == -1) moveStyle = (mask != null) ? 2 : 0;
        if (idleStyle == -1) idleStyle = (mask != null) ? 2 : 0;
    }

    private static String debugBytes(byte[] data, int pos, int span) {
        int from = Math.max(0, pos - span);
        int to = Math.min(data.length, pos + span);
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(from).append("..").append(to).append("] ");
        for (int i = from; i < to; i++) {
            if (i == pos) {
                sb.append("<");
            }
            int v = data[i] & 0xFF;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v).toUpperCase());
            if (i == pos) {
                sb.append(">");
            }
            if (i + 1 < to) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

}
