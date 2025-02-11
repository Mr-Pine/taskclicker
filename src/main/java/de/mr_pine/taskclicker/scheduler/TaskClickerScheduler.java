package de.mr_pine.taskclicker.scheduler;

import me.bechberger.ebpf.annotations.Unsigned;
import me.bechberger.ebpf.annotations.bpf.BPF;
import me.bechberger.ebpf.annotations.bpf.Property;
import me.bechberger.ebpf.bpf.BPFProgram;
import me.bechberger.ebpf.bpf.Scheduler;
import me.bechberger.ebpf.runtime.TaskDefinitions;
import me.bechberger.ebpf.type.Ptr;

import static me.bechberger.ebpf.runtime.ScxDefinitions.*;

@BPF(license = "GPL")
@Property(name = "sched_name", value = "taskclicker")
public abstract class TaskClickerScheduler extends BPFProgram implements Scheduler {

    @Override
    public void enqueue(Ptr<TaskDefinitions.task_struct> p, long enq_flags) {
        @Unsigned int baseSlice = 5_000_000;
        scx_bpf_dispatch(p, scx_dsq_id_flags.SCX_DSQ_GLOBAL.value(), baseSlice, enq_flags);
    }

    public static void aaaa() {
        try (var program = BPFProgram.load(TaskClickerScheduler.class)) {
            program.attachScheduler();
            System.out.println("Attached scheduler");
            while (true) {
                program.consumeAndThrow();
            }
        }
    }
}
