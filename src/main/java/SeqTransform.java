import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SeqTransform} contains the {@link SeqSkeleton} and <code>x,y,z</code> parameters to transform a {@link Model}.
 *
 * @see Model#applyTransform(int)
 * @see Model#applyTransforms(int, int, int[])
 */
public class SeqTransform {

    public static SeqTransform[] instances;
    private static Map<Integer, SeqTransform> extendedInstances;

    /**
     * Syntax sugar.
     *
     * @param id the id
     * @return <code>id == -1</code>
     */
    public static boolean isNull(int id) {
        return id == -1;
    }

    /**
     * Initializes the array of instances.
     *
     * @param count the count
     */
    public static void init(int count) {
        instances = new SeqTransform[Math.max(count + 1, 65536)];
        extendedInstances = new HashMap<>();
    }

    /**
     * Nullifies the array of instances.
     */
    public static void unload() {
        instances = null;
        extendedInstances = null;
    }

    /**
     * Gets the {@link SeqTransform}
     *
     * @param id the transform id.
     * @return the {@link SeqTransform} or <code>null</code> if it does not exist.
     */
    public static SeqTransform get(int id) {
        if ((id < 0) || (instances == null)) {
            return null;
        }
        if (id < instances.length) {
            return instances[id];
        }
        if (extendedInstances != null) {
            return extendedInstances.get(id);
        }
        return null;
    }

    private static void put(int id, SeqTransform transform) {
        if (id < 0) {
            return;
        }
        if (id < instances.length) {
            instances[id] = transform;
            return;
        }
        if (extendedInstances == null) {
            extendedInstances = new HashMap<>();
        }
        extendedInstances.put(id, transform);
    }

    /**
     * Legacy compatibility overload.
     */
    public static void unpack(byte[] src) {
        unpack(-1, src);
    }

    /**
     * Loads a frame-group, matching the common "Class36.load(file, data)" format:
     * skeleton + frameCount + [localFrameId + transformPayload...]
     */
    public static void unpack(int file, byte[] src) {
        if ((src == null) || (src.length == 0)) {
            return;
        }
        try {
            Buffer in = new Buffer(src);
            SeqSkeleton skeleton = SeqSkeleton.unpackModern(in);
            int frameCount = in.readU16();

            int[] bases = new int[500];
            int[] x = new int[500];
            int[] y = new int[500];
            int[] z = new int[500];

            for (int frame = 0; frame < frameCount; frame++) {
                if (in.position + 3 > src.length) {
                    break;
                }
                int localId = in.readU16();
                int transformId = file >= 0 ? ((file << 16) | localId) : localId;

                SeqTransform transform = new SeqTransform();
                transform.skeleton = skeleton;

                int baseCount = in.readU8();
                int length = 0;
                int lastBase = -1;

                for (int base = 0; base < baseCount; base++) {
                    if (in.position >= src.length) {
                        break;
                    }
                    int flags = in.readU8();
                    if (flags <= 0) {
                        continue;
                    }
                    if (base >= skeleton.baseTypes.length) {
                        if ((flags & 1) != 0 && (in.position + 2) <= src.length) in.readShort2();
                        if ((flags & 2) != 0 && (in.position + 2) <= src.length) in.readShort2();
                        if ((flags & 4) != 0 && (in.position + 2) <= src.length) in.readShort2();
                        continue;
                    }

                    if (skeleton.baseTypes[base] != SeqSkeleton.OP_BASE) {
                        for (int cur = base - 1; cur > lastBase; cur--) {
                            if ((cur < skeleton.baseTypes.length) && (skeleton.baseTypes[cur] == SeqSkeleton.OP_BASE)) {
                                bases[length] = cur;
                                x[length] = 0;
                                y[length] = 0;
                                z[length] = 0;
                                length++;
                                break;
                            }
                        }
                    }

                    bases[length] = base;
                    int defaultValue = skeleton.baseTypes[base] == SeqSkeleton.OP_SCALE ? 128 : 0;
                    x[length] = (flags & 1) != 0 ? in.readShort2() : defaultValue;
                    y[length] = (flags & 2) != 0 ? in.readShort2() : defaultValue;
                    z[length] = (flags & 4) != 0 ? in.readShort2() : defaultValue;
                    lastBase = base;
                    length++;
                }

                transform.length = length;
                transform.bases = new int[length];
                transform.x = new int[length];
                transform.y = new int[length];
                transform.z = new int[length];
                for (int i = 0; i < length; i++) {
                    transform.bases[i] = bases[i];
                    transform.x[i] = x[i];
                    transform.y[i] = y[i];
                    transform.z[i] = z[i];
                }

                put(transformId, transform);
            }
        } catch (Exception ignored) {
            // Keep client alive on malformed frame groups.
        }
    }

    /**
     * The skeleton associated to this transform.
     */
    public SeqSkeleton skeleton;
    /**
     * The delay in <code>ticks</code>.
     */
    public int delay;
    /**
     * The number of operations this transform performs.
     */
    public int length;
    /**
     * The list of bases this transform uses.
     */
    public int[] bases;
    /**
     * This transforms parameters.
     */
    public int[] x, y, z;

}
