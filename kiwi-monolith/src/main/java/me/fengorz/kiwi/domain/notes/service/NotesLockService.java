/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.notes.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import me.fengorz.kiwi.domain.notes.dto.NotesPasscodeRequest;
import me.fengorz.kiwi.domain.notes.dto.NotesUnlockRequest;
import me.fengorz.kiwi.domain.notes.entity.NotesUserLock;
import me.fengorz.kiwi.domain.notes.mapper.NotesUserLockMapper;
import me.fengorz.kiwi.domain.notes.vo.NotesLockStatusVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Notes Lock Service - Manages passcode and lock state for notes feature
 *
 * @author codingByFeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotesLockService extends ServiceImpl<NotesUserLockMapper, NotesUserLock> {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Get lock status for a user
     */
    public NotesLockStatusVO getLockStatus(Integer userId) {
        NotesUserLock lock = getById(userId);

        if (lock == null) {
            return NotesLockStatusVO.builder()
                    .hasPasscode(false)
                    .isLocked(false)
                    .failedAttempts(0)
                    .build();
        }

        return NotesLockStatusVO.builder()
                .hasPasscode(true)
                .isLocked(Boolean.TRUE.equals(lock.getIsLocked()))
                .lockTime(lock.getLockTime())
                .failedAttempts(lock.getFailedAttempts())
                .build();
    }

    /**
     * Set or change passcode
     */
    @Transactional
    public void setPasscode(NotesPasscodeRequest request, Integer userId) {
        NotesUserLock lock = getById(userId);

        if (lock != null) {
            // Changing existing passcode - verify current passcode
            if (request.getCurrentPasscode() == null) {
                throw new ServiceException("Current passcode is required to change passcode");
            }
            if (!passwordEncoder.matches(request.getCurrentPasscode(), lock.getPasscodeHash())) {
                throw new ServiceException("Current passcode is incorrect");
            }

            lock.setPasscodeHash(passwordEncoder.encode(request.getNewPasscode()));
            lock.setUpdateTime(LocalDateTime.now());
            updateById(lock);

            log.info("Passcode changed for user {}", userId);
        } else {
            // Setting initial passcode
            lock = NotesUserLock.builder()
                    .userId(userId)
                    .passcodeHash(passwordEncoder.encode(request.getNewPasscode()))
                    .isLocked(false)
                    .failedAttempts(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            save(lock);

            log.info("Passcode set for user {}", userId);
        }
    }

    /**
     * Lock notes for a user
     */
    @Transactional
    public void lock(Integer userId) {
        NotesUserLock lock = getById(userId);

        if (lock == null) {
            throw new ServiceException("Please set a passcode first before locking");
        }

        lock.setIsLocked(true);
        lock.setLockTime(LocalDateTime.now());
        lock.setUpdateTime(LocalDateTime.now());
        updateById(lock);

        log.info("Notes locked for user {}", userId);
    }

    /**
     * Unlock notes with passcode
     */
    @Transactional
    public void unlock(NotesUnlockRequest request, Integer userId) {
        NotesUserLock lock = getById(userId);

        if (lock == null) {
            throw new ServiceException("No passcode configured");
        }

        // Check if user is in lockout period
        if (lock.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockoutEnd = lock.getLastFailedTime().plusMinutes(LOCKOUT_MINUTES);
            if (LocalDateTime.now().isBefore(lockoutEnd)) {
                throw new ServiceException("Too many failed attempts. Please try again later.");
            }
            // Reset failed attempts after lockout period
            lock.setFailedAttempts(0);
        }

        // Verify passcode
        if (!passwordEncoder.matches(request.getPasscode(), lock.getPasscodeHash())) {
            lock.setFailedAttempts(lock.getFailedAttempts() + 1);
            lock.setLastFailedTime(LocalDateTime.now());
            lock.setUpdateTime(LocalDateTime.now());
            updateById(lock);

            int remaining = MAX_FAILED_ATTEMPTS - lock.getFailedAttempts();
            if (remaining > 0) {
                throw new ServiceException("Incorrect passcode. " + remaining + " attempts remaining.");
            } else {
                throw new ServiceException("Too many failed attempts. Please try again in " + LOCKOUT_MINUTES + " minutes.");
            }
        }

        // Passcode correct - unlock
        lock.setIsLocked(false);
        lock.setLockTime(null);
        lock.setFailedAttempts(0);
        lock.setLastFailedTime(null);
        lock.setUpdateTime(LocalDateTime.now());
        updateById(lock);

        log.info("Notes unlocked for user {}", userId);
    }

    /**
     * Remove passcode (requires current passcode)
     */
    @Transactional
    public void removePasscode(String currentPasscode, Integer userId) {
        NotesUserLock lock = getById(userId);

        if (lock == null) {
            throw new ServiceException("No passcode configured");
        }

        if (!passwordEncoder.matches(currentPasscode, lock.getPasscodeHash())) {
            throw new ServiceException("Incorrect passcode");
        }

        removeById(userId);
        log.info("Passcode removed for user {}", userId);
    }

    /**
     * Check if notes are locked for a user
     */
    public boolean isLocked(Integer userId) {
        NotesUserLock lock = getById(userId);
        return lock != null && Boolean.TRUE.equals(lock.getIsLocked());
    }

    /**
     * Verify that notes are unlocked - throws exception if locked
     */
    public void verifyUnlocked(Integer userId) {
        if (isLocked(userId)) {
            throw new ServiceException("Notes are locked. Please unlock to access.");
        }
    }
}
