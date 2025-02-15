package de.mr_pine.taskclicker.scheduler;

import me.bechberger.ebpf.annotations.Size;
import me.bechberger.ebpf.annotations.Type;
import me.bechberger.ebpf.annotations.Unsigned;
import me.bechberger.ebpf.annotations.bpf.BPF;
import me.bechberger.ebpf.annotations.bpf.BPFFunction;
import me.bechberger.ebpf.annotations.bpf.BPFMapDefinition;
import me.bechberger.ebpf.annotations.bpf.Property;
import me.bechberger.ebpf.bpf.BPFProgram;
import me.bechberger.ebpf.bpf.GlobalVariable;
import me.bechberger.ebpf.bpf.Scheduler;
import me.bechberger.ebpf.bpf.map.BPFBloomFilter;
import me.bechberger.ebpf.bpf.map.BPFQueue;
import me.bechberger.ebpf.runtime.BpfDefinitions;
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
@Property(name = "sched_name", value = "taskclicker")
@Property(name = "timeout_ms", value = "2500")
public abstract class TaskClickerScheduler extends BPFProgram implements Scheduler, SystemCallHooks {

    @Type
    public record ClickableTask(int pid, int tgid, int leaderPid, int leaderTgid, long enqFlags, @Size(16) byte[] comm,
                                @Unsigned long nsEntry) {
    }

    final GlobalVariable<Integer> syscalls = new GlobalVariable<>(0);
    final GlobalVariable<Boolean> running = new GlobalVariable<>(false);

    @BPFMapDefinition(maxEntries = 4069)
    BPFQueue<ClickableTask> enqueued;
    @BPFMapDefinition(maxEntries = 4069)
    BPFQueue<Integer> dispatched;

    private static final int MAX_BLACKLIST_ENTRIES = 100;
    @BPFMapDefinition(maxEntries = MAX_BLACKLIST_ENTRIES)
    BPFBloomFilter<Integer> pidBlacklist;

    private static final int BLACKLISTED_DSQ_ID = 1;

    @BPFFunction(
            headerTemplate = "int BPF_PROG($name, struct pt_regs *regs, unsigned long number)",
            lastStatement = "return 0;",
            section = "raw_tracepoint/sys_enter",
            autoAttach = true
    )
    public void syscall_counter(Ptr<PtDefinitions.pt_regs> regs, @Unsigned long number) {
        syscalls.set(syscalls.get() + 1);
    }

    @Override
    public int init() {
        running.set(true);
        return scx_bpf_create_dsq(BLACKLISTED_DSQ_ID, -1);
    }

    @Override
    public void exit(Ptr<scx_exit_info> ei) {
        running.set(false); // TODO: Check for stall
    }

    @Override
    public void enqueue(Ptr<TaskDefinitions.task_struct> p, long enq_flags) {
        int pid = p.val().pid;
        boolean is_blacklisted = pidBlacklist.peek(p.val().pid) || pidBlacklist.peek(p.val().tgid) || pidBlacklist.peek(p.val().group_leader.val().pid) || pidBlacklist.peek(p.val().group_leader.val().tgid);

        boolean isEnqueued = false;
        if (!is_blacklisted) {
            ClickableTask clickableTask = new ClickableTask(pid, p.val().tgid, p.val().group_leader.val().pid, p.val().group_leader.val().tgid, enq_flags, "hello".getBytes(), bpf_ktime_get_tai_ns());
            bpf_probe_read_kernel_str(Ptr.of(clickableTask.comm), 16, Ptr.of(p.val().comm));
            if (enqueued.push(clickableTask)) isEnqueued = true;
        }

        if (!isEnqueued) {
            @Unsigned int baseSlice = 5_000_000;
            scx_bpf_dispatch(p, BLACKLISTED_DSQ_ID, baseSlice / scx_bpf_dsq_nr_queued(BLACKLISTED_DSQ_ID), 1033);
        }
    }

    @Override
    public void stopping(Ptr<TaskDefinitions.task_struct> p, boolean runnable) {
    }

    @Override
    public void dispatch(int cpu, Ptr<TaskDefinitions.task_struct> prev) {
        int pid = 0;
        for (int i = 0; i < 10 && dispatched.bpf_pop(pid); i++) {
            Ptr<TaskDefinitions.task_struct> task = bpf_task_from_pid(pid);
            if (task != null) {
                @Unsigned int baseSlice = 5_000_000;
                scx_bpf_dispatch(task, scx_dsq_id_flags.SCX_DSQ_GLOBAL.value(), baseSlice / scx_bpf_dsq_nr_queued(scx_dsq_id_flags.SCX_DSQ_GLOBAL.value()), 0);
                bpf_task_release(task);
            }
        }

        scx_bpf_consume(BLACKLISTED_DSQ_ID); // TODO: After 6.15: renamed to scx_bpf_dsq_move_to_local
    }

    void queueLoop(Consumer<ClickableTask> taskConsumer, IntConsumer syscallUpdater) {
        while (running.get()) {
            consumeAndThrow();
            ClickableTask task = enqueued.pop();
            while (task != null) {
                taskConsumer.accept(task);
                task = enqueued.pop();
                syscallUpdater.accept(syscalls.get());
                syscalls.set(0);
            }
        }
    }

    int scheduleInsertCount = 0;

    public void schedule(int pid) {
        dispatched.push(pid);
        scheduleInsertCount++;
    }

    public static void run(Consumer<ClickableTask> taskConsumer, Consumer<TaskClickerScheduler> returner, IntConsumer syscallUpdater, int[] pidBlacklist) {
        try (var program = BPFProgram.load(TaskClickerScheduler.class)) {
            for (int pid : pidBlacklist) {
                program.pidBlacklist.put(pid);
            }
            returner.accept(program);

            program.rawTracepointAttach("syscall_counter", "sys_enter");
            program.attachScheduler();
            System.out.println("Attached scheduler");
            program.queueLoop(taskConsumer, syscallUpdater);
        }
        System.out.println("Scheduler exited");
    }
}
