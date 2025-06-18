package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pandaterry.concurrent_entity_change_logger.core.domain.LogEntry;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage.Checkpoint;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.storage.LogStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DiskLogWriter implements LogStorage {
    private static final int BUFFER_SIZE = 8192;
    private static final String LOG_FILE_PATH = "logs/entity-changes.log";
    private static final String CHECKPOINT_FILE = "checkpoint.json";
    private static final String BACKUP_SUFFIX = ".backup";

    private final ObjectMapper objectMapper;
    private final Path logFilePath;
    private final AtomicLong filePosition;
    private RandomAccessFile logFile;
    private BufferedOutputStream bufferedOutput;
    private FileChannel channel;
    private FileLock lock;

    public DiskLogWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.logFilePath = Paths.get(LOG_FILE_PATH);
        this.filePosition = new AtomicLong(0);
    }

    @Override
    public void init() throws IOException {
        Files.createDirectories(logFilePath.getParent());

        // 파일 잠금 획득
        this.logFile = new RandomAccessFile(logFilePath.toFile(), "rw");
        this.channel = logFile.getChannel();

        try{
            this.lock = channel.tryLock(0L, Long.MAX_VALUE, false);
        }catch (OverlappingFileLockException e){
            throw new RuntimeException("JVM 에서 이미 락이 걸려있어서 파일 락을 획득할 수 없습니다. " + e);
        }
        if (lock == null) {
            throw new IOException("다른 프로세스가 락을 갖고 있어서 락을 획득할 수 없습니다.");
        }

        // 버퍼링 설정
        this.bufferedOutput = new BufferedOutputStream(
                Channels.newOutputStream(channel), BUFFER_SIZE);

        // 체크포인트 로드
        loadCheckpoint();
    }

    @Override
    public synchronized void write(LogEntry entry) throws IOException {
        try {
            String json = objectMapper.writeValueAsString(entry) + "\n";
            byte[] data = json.getBytes();

            // 버퍼에 쓰기
            bufferedOutput.write(data);
            bufferedOutput.flush();

            // 위치 업데이트
            filePosition.addAndGet(data.length);

            // 체크포인트 저장
            saveCheckpoint();
        } catch (IOException e) {
            // 파일 손상 감지 및 복구
            if (isFileCorrupted()) {
                recoverFromBackup();
            }
            throw e;
        }
    }

    private boolean isFileCorrupted() {
        try {
            // 파일 크기와 체크포인트 위치 비교
            long fileSize = logFile.length();
            return fileSize < filePosition.get();
        } catch (IOException e) {
            return true;
        }
    }

    private void recoverFromBackup() throws IOException {
        Path backupPath = logFilePath.resolveSibling(logFilePath.getFileName() + BACKUP_SUFFIX);

        // 백업 파일이 있으면 복구
        if (Files.exists(backupPath)) {
            // 현재 파일 백업
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            Path corruptedBackup = logFilePath.resolveSibling("corrupted-" + timestamp + ".log");
            Files.move(logFilePath, corruptedBackup);

            // 백업에서 복구
            Files.copy(backupPath, logFilePath, StandardCopyOption.REPLACE_EXISTING);

            // 리소스 재초기화
            close();
            init();
        } else {
            throw new IOException("No backup file available for recovery");
        }
    }

    private void saveCheckpoint() throws IOException {
        Checkpoint checkpoint = new Checkpoint(
                filePosition.get(),
                LocalDateTime.now());
        objectMapper.writeValue(new File(CHECKPOINT_FILE), checkpoint);

        // 백업 파일 생성
        Path backupPath = logFilePath.resolveSibling(logFilePath.getFileName() + BACKUP_SUFFIX);
        Files.copy(logFilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void loadCheckpoint() throws IOException {
        File checkpointFile = new File(CHECKPOINT_FILE);
        if (checkpointFile.exists()) {
            Checkpoint checkpoint = objectMapper.readValue(checkpointFile, Checkpoint.class);
            filePosition.set(checkpoint.getPosition());
        }
    }

    @Override
    public void close() {
        try {
            if (bufferedOutput != null) {
                bufferedOutput.close();
            }
            if (lock != null) {
                lock.release();
            }
            if (channel != null) {
                channel.close();
            }
            if (logFile != null) {
                logFile.close();
            }
        } catch (IOException e) {
            log.error("Failed to close resources", e);
        }
    }
}