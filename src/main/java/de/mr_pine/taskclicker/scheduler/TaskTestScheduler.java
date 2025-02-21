package de.mr_pine.taskclicker.scheduler;

import me.bechberger.ebpf.annotations.Size;
import me.bechberger.ebpf.annotations.Type;
import me.bechberger.ebpf.annotations.Unsigned;
import me.bechberger.ebpf.annotations.bpf.BPF;
import me.bechberger.ebpf.annotations.bpf.BPFFunction;
import me.bechberger.ebpf.annotations.bpf.BPFMapDefinition;
import me.bechberger.ebpf.annotations.bpf.Property;
import me.bechberger.ebpf.bpf.BPFJ;
import me.bechberger.ebpf.bpf.BPFProgram;
import me.bechberger.ebpf.bpf.GlobalVariable;
import me.bechberger.ebpf.bpf.Scheduler;
import me.bechberger.ebpf.bpf.map.BPFBloomFilter;
import me.bechberger.ebpf.bpf.map.BPFQueue;
import me.bechberger.ebpf.runtime.PtDefinitions;
import me.bechberger.ebpf.runtime.TaskDefinitions;
import me.bechberger.ebpf.runtime.interfaces.SystemCallHooks;
import me.bechberger.ebpf.type.Ptr;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static me.bechberger.ebpf.runtime.BpfDefinitions.bpf_task_from_pid;
import static me.bechberger.ebpf.runtime.BpfDefinitions.bpf_task_release;
import static me.bechberger.ebpf.runtime.ScxDefinitions.*;
import static me.bechberger.ebpf.runtime.helpers.BPFHelpers.*;

@BPF(license = "GPL")
@Property(name = "sched_name", value = "taskclicker_test")
@Property(name = "timeout_ms", value = "5000")
public abstract class TaskTestScheduler extends BPFProgram implements Scheduler {

    final GlobalVariable<Boolean> running = new GlobalVariable<>(true);

    private static final int MAX_BLACKLIST_ENTRIES = 100;
    @BPFMapDefinition(maxEntries = MAX_BLACKLIST_ENTRIES)
    BPFBloomFilter<Integer> pidBlacklist;

    @BPFFunction
    private boolean isBlacklisted(Ptr<TaskDefinitions.task_struct> task) {
        return pidBlacklist.peek(task.val().pid) || pidBlacklist.peek(task.val().tgid) || pidBlacklist.peek(task.val().group_leader.val().pid) || pidBlacklist.peek(task.val().group_leader.val().tgid);
    }

    @Override
    public void exit(Ptr<scx_exit_info> ei) {
        running.set(false); // TODO: Check for stall
    }

    @Override
    public void enqueue(Ptr<TaskDefinitions.task_struct> p, long enq_flags) {
        boolean blacklisted = isBlacklisted(p);
        if (blacklisted) {
            @Unsigned int baseSlice = 5_000_000;
            scx_bpf_dispatch(p, scx_dsq_id_flags.SCX_DSQ_GLOBAL.value(), baseSlice / scx_bpf_dsq_nr_queued(scx_dsq_id_flags.SCX_DSQ_GLOBAL.value()), 1033);
        }
    }

    public static void run(int[] pidBlacklist) {
        try (var program = BPFProgram.load(TaskTestScheduler.class)) {
            for (int pid : pidBlacklist) {
                program.pidBlacklist.put(pid);
            }

            program.attachScheduler();
            System.out.println("Attached scheduler");
            while (program.running.get()) {
                Thread.yield();
            }
        }
        System.out.println("Scheduler exited");
    }
}
