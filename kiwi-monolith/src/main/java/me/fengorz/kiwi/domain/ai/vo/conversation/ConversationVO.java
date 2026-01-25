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
package me.fengorz.kiwi.domain.ai.vo.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.fengorz.kiwi.domain.ai.entity.conversation.Conversation;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Conversation VO for API responses
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private Long id;
    private String topic;
    private String prompt;
    private String accent;
    private Integer durationMinutes;
    private Integer speakerCount;
    private String status;
    private Integer totalMessages;
    private Long totalAudioDurationMs;
    private List<SpeakerVO> speakers;
    private List<MessageVO> messages;
    private LocalDateTime createTime;

    public static ConversationVO fromEntity(Conversation entity) {
        if (entity == null) {
            return null;
        }

        List<SpeakerVO> speakers = entity.getSpeakers() != null
                ? entity.getSpeakers().stream().map(SpeakerVO::fromEntity).collect(Collectors.toList())
                : Collections.emptyList();

        List<MessageVO> messages = entity.getMessages() != null
                ? entity.getMessages().stream().map(MessageVO::fromEntity).collect(Collectors.toList())
                : Collections.emptyList();

        return ConversationVO.builder()
                .id(entity.getId())
                .topic(entity.getTopic())
                .prompt(entity.getPrompt())
                .accent(entity.getAccent())
                .durationMinutes(entity.getDurationMinutes())
                .speakerCount(entity.getSpeakerCount())
                .status(entity.getStatus())
                .totalMessages(entity.getTotalMessages())
                .totalAudioDurationMs(entity.getTotalAudioDurationMs())
                .speakers(speakers)
                .messages(messages)
                .createTime(entity.getCreateTime())
                .build();
    }
}
