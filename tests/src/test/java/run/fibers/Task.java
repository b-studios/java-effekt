package run.fibers;

import effekt.*;

public interface Task extends Prog<Void> {
    void run() throws Effects;
    default Void apply() throws Effects { run(); return null; }
}