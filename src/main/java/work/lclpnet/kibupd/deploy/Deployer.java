package work.lclpnet.kibupd.deploy;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class Deployer {

    private final ArtifactCollector collector;
    private final TargetMapper mapper;
    private final FileTransfer transfer;
    private final Executor executor;

    public Deployer(ArtifactCollector collector, TargetMapper mapper, FileTransfer transfer) {
        this(collector, mapper, transfer, ForkJoinPool.commonPool());
    }

    public Deployer(ArtifactCollector collector, TargetMapper mapper, FileTransfer transfer, Executor executor) {
        this.collector = collector;
        this.mapper = mapper;
        this.transfer = transfer;
        this.executor = executor;
    }

    public CompletableFuture<Void> deploy() {
        return CompletableFuture.runAsync(this::deploySync, executor);
    }

    public void deploySync() {
        collector.collect().forEach(this::deploy);
    }

    protected void deploy(Path artifact) {
        Path target = mapper.map(artifact);
        transfer.transfer(artifact, target);
    }
}
