package yamahari.codingchallenge;

import java.util.*;

public final class Main {
    private Main() {
    }

    /* utility classes */

    /* solve single 0/1 knapsack using dynamic programming */
    private static SingleKnapsackResult singleKnapsack(final Item[] items, final List<Integer> indices, final int c) {
        final SingleKnapsackResult result = new SingleKnapsackResult();

        final int size = indices.size();
        final int[][] dp = new int[size + 1][c + 1];

        for (int k = 0; k <= c; ++k) {
            dp[0][k] = 0;
        }

        for (int j = 1; j <= size; ++j) {
            for (int k = 0; k <= c; ++k) {
                final Item item = items[indices.get(j - 1)];
                dp[j][k] = item.w > k
                        ? dp[j - 1][k]
                        : Math.max(dp[j - 1][k], item.p + dp[j - 1][k - item.w]);
            }
        }

        /* backtrack to get the items used in the optimal solution */
        int z = dp[size][c];
        int w = c;
        result.z = z;
        result.x = new int[items.length];
        for (int j = size; j > 0; --j) {
            if (z != dp[j - 1][w]) {
                final int jj = indices.get(j - 1);
                final Item item = items[jj];
                result.x[jj] = 1;
                z -= item.p;
                w -= item.w;
            }
        }

        return result;
    }

    /* Computes the lower bound for the bound and bound algorithm */
    private static LowerResult lower(final int n, final int m, final Item[] items, final int[] cs, final int[][] x,
                                     final List<Stack<Integer>> ss, final int i) {
        final LowerResult result = new LowerResult();
        result.L = 0;
        result.x = new int[m][n];
        for (int k = 0; k <= i; ++k) {
            for (final int j : ss.get(k)) {
                result.L += items[j].p * x[k][j];
            }
        }
        final Set<Integer> indices = new HashSet<>();
        for (int k = 0; k <= i; ++k) {
            for (int j = 0; j < n; ++j) {
                if (x[k][j] == 0) {
                    indices.add(j);
                }
            }
        }
        indices.removeAll(ss.get(i));

        int c = cs[i];
        for (final int j : ss.get(i)) {
            c -= items[j].w * x[i][j];
        }

        int k = i;
        do {
            final SingleKnapsackResult singleKnapsack = singleKnapsack(items, new ArrayList<>(indices), c);
            result.L += singleKnapsack.z;
            result.x[k] = singleKnapsack.x;
            for (int j = 0; j < n; ++j) {
                if (result.x[k][j] == 1) {
                    indices.remove(j);
                }
            }
            ++k;
            if (k < m) {
                c = cs[k];
            }
        } while (k < m);

        return result;
    }

    /* Computes the upper bound for the bound and bound algorithm */
    private static UpperResult upper(final int n, final int m, final Item[] items, final int[] cs, final int[][] x,
                                     final List<Stack<Integer>> ss, final int i) {
        final UpperResult result = new UpperResult();

        int c = cs[i];
        for (int j : ss.get(i)) {
            c -= items[j].w * x[i][j];
        }

        for (int k = i + 1; k < m; ++k) {
            c += cs[k];
        }

        final List<Integer> indices = new ArrayList<>();
        for (int k = 0; k <= i; ++k) {
            for (int j = 0; j < n; ++j) {
                if (x[k][j] == 0) {
                    indices.add(j);
                }
            }
        }

        final SingleKnapsackResult singleKnapsack = singleKnapsack(items, indices, c);

        result.U = singleKnapsack.z;
        for (int k = 0; k <= i; ++k) {
            for (final int j : ss.get(k)) {
                result.U += items[j].p * x[k][j];
            }
        }

        return result;
    }

    /* Compute the optimal solution to the multiple 0/1 knapsack problem */
    private static MultipleKnapsackResult multipleKnapsack(final int n, final int m, final Item[] items, final int[] cs) {
        final MultipleKnapsackResult result = new MultipleKnapsackResult();
        final List<Stack<Integer>> ss = new ArrayList<>();
        int[][] x = new int[m][n];
        int i;
        UpperResult upper;
        LowerResult lower;
        int UB;

        // single knapsack
        if (m == 1) {
            final List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < n; ++j) {
                indices.add(j);
            }
            final SingleKnapsackResult singleKnapsackResult = singleKnapsack(items, indices, cs[0]);
            result.x = new int[m][n];
            result.x[0] = singleKnapsackResult.x;
            result.z = singleKnapsackResult.z;
            return result;
        }

        /* step 1 - initialize */
        for (int k = 0; k < m; ++k) {
            ss.add(new Stack<>());
        }
        for (int k = 0; k < m; ++k) {
            for (int j = 0; j < n; ++j) {
                x[k][j] = 0;
            }
        }
        result.z = 0;
        result.x = new int[m][n];
        i = 0;
        upper = upper(n, m, items, cs, x, ss, i);
        UB = upper.U;
        lower = new LowerResult(); // DUMMY, might wanna check if this gets used accidentally

        int step = 2;
        while (true) {
            begin:
            switch (step) {
                /* step 2 - heuristic */
                case 2: {
                    lower = lower(n, m, items, cs, x, ss, i);
                    if (lower.L > result.z) {
                        result.z = lower.L;
                        for (int k = 0; k < m; ++k) {
                            System.arraycopy(x[k], 0, result.x[k], 0, n);
                        }
                        for (int k = i; k < m; ++k) {
                            for (int j = 0; j < n; ++j) {
                                if (lower.x[k][j] == 1) {
                                    result.x[k][j] = lower.x[k][j];
                                }
                            }
                        }
                        if (result.z == UB) {
                            return result;
                        }
                        if (result.z == upper.U) {
                            step = 4;
                            break;
                        }
                    }
                    step = 3;
                    break;
                }
                /* step 3 - define a new current solution */
                case 3: {
                    do {
                        final Set<Integer> I = new HashSet<>();
                        for (int l = 0; l < n; ++l) {
                            if (lower.x[i][l] == 1) {
                                I.add(l);
                            }
                        }
                        while (!I.isEmpty()) {
                            int j = Collections.min(I);
                            I.remove(j);
                            ss.get(i).push(j);
                            x[i][j] = 1;
                            upper = upper(n, m, items, cs, x, ss, i);
                            if (upper.U <= result.z) {
                                step = 4;
                                break begin;
                            }
                        }
                        ++i;
                    } while (i != m - 1);
                    i = m - 2;
                    step = 4;
                    break;
                }
                /* step 4 - backtrack */
                case 4: {
                    do {
                        while (!ss.get(i).isEmpty()) {
                            int j = ss.get(i).peek();
                            if (x[i][j] == 0) {
                                ss.get(i).pop();
                            } else {
                                x[i][j] = 0;
                                upper = upper(n, m, items, cs, x, ss, i);
                                if (upper.U > result.z) {
                                    step = 2;
                                    break begin;
                                }
                            }
                        }
                        --i;
                    } while (i != -1);
                }
                default:
                    return null;
            }
        }
    }

    /*The requested implementation is an instance of the multiple bounded knapsack problem
    The provided implementation is the bound and bound algorithm by Silvano Martello and Paolo Toth published in 1980.
    (https://www.sciencedirect.com/science/article/pii/0166218X81900056)
    Because that algorithm solves multiple 0/1 knapsack, we'll first have to transform the bounded version into
    an equivalent 0/1 version.
    */
    public static void main(final String[] args) {
        /* provided input data */
        String[] names = {"Notebook Büro 13\"", "Notebook Büro 14\"", "Notebook outdoor", "Mobiltelefon Büro",
                "Mobiltelefon Outdoor", "Mobiltelefon Heavy Duty", "Tablet Büro klein", "Tabel Büro groß",
                "Tablet outdoor klein", "Tablet outdoor groß"};
        int[] weights = {2451, 2978, 3625, 717, 988, 1220, 1405, 1455, 1690, 1980};
        int[] profits = {40, 35, 80, 30, 60, 65, 40, 40, 45, 68};
        int[] bounds = {205, 420, 450, 60, 157, 220, 620, 250, 540, 370};

        /* capacities of the 2 knapsacks ( transporters ) !sorted ascending! */
        int[] capacities = {1_100_000 - 85_700, 1_100_000 - 72_400};

        /* transform multiple bounded knapsack into multiple 0/1 knapsack */
        final List<Item> items = new ArrayList<>();
        for (int j = 0; j < 10; ++j) {
            int beta = 0;
            int k = 1;
            do {
                if (beta + k > bounds[j]) {
                    k = bounds[j] - beta;
                }
                items.add(new Item(k * profits[j], k * weights[j], j, k));
                beta += k;
                k <<= 1;

            } while (beta != bounds[j]);
        }

        /* sort items by profit/weight !descending! */
        items.sort(Collections.reverseOrder(
                (i0, i1) -> Float.compare((float) i0.p / (float) i0.w, (float) i1.p / (float) i1.w)));

        /* solve and print the solution */
        final MultipleKnapsackResult multipleKnapsack =
                multipleKnapsack(items.size(), capacities.length, items.toArray(Item[]::new), capacities);

        if (multipleKnapsack != null) {
            final Map<Integer, Integer> k0 = new HashMap<>(), k1 = new HashMap<>();

            for (int i = 0; i < items.size(); ++i) {
                if (multipleKnapsack.x[0][i] == 1) {
                    final Integer v = k0.get(items.get(i).idx);
                    k0.put(items.get(i).idx, v == null ? items.get(i).multiplier : v + items.get(i).multiplier);
                }
                if (multipleKnapsack.x[1][i] == 1) {
                    final Integer v = k1.get(items.get(i).idx);
                    k1.put(items.get(i).idx, v == null ? items.get(i).multiplier : v + items.get(i).multiplier);
                }
                /* For debug purposes */
                if (multipleKnapsack.x[0][i] == 1 && multipleKnapsack.x[1][i] == 1) {
                    throw new RuntimeException("Same item in two different bags.");
                }
            }

            System.out.printf("Die optimale Beladung hat einen Nutzwert von %d. Sie lautet:\n", multipleKnapsack.z);
            System.out.println("Transporter 1 - Fahrer 85,7 kg:");
            printKnapsack(names, weights, profits, k0, 85_700);
            System.out.println("\nTransporter 2 - Fahrer 72,4 kg:");
            printKnapsack(names, weights, profits, k1, 72_400);
        } else {
            System.out.println("Something went wrong!");
        }
    }

    private static void printKnapsack(final String[] names, final int[] weights, final int[] profits,
                                      final Map<Integer, Integer> k, int w) {
        int p = 0;
        for (int i = 0; i < 10; ++i) {
            final int count = k.getOrDefault(i, 0);
            w += weights[i] * count;
            p += profits[i] * count;
            System.out.printf("%d. %d x %s\n", i + 1, count, names[i]);
        }
        System.out.printf("Gesamtnutzwert = %d, Gesamtgewicht(in g) = %d\n", p, w);
    }

    private static class Item {
        public final int p;
        public final int w;
        public final int idx;
        public final int multiplier;

        public Item(final int p, final int w, final int idx, final int multiplier) {
            this.p = p;
            this.w = w;
            this.idx = idx;
            this.multiplier = multiplier;
        }
    }

    private static class SingleKnapsackResult {
        public int z;
        public int[] x;
    }

    private static class LowerResult {
        public int L;
        public int[][] x;
    }

    private static class UpperResult {
        public int U;
    }

    private static class MultipleKnapsackResult {
        public int z;
        public int[][] x;
    }
}
