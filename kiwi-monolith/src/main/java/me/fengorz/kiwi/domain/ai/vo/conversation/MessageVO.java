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
import me.fengorz.kiwi.domain.ai.entity.conversation.ConversationMessage;

/**
 * Message VO for API responses
 *
 * @author codingByFeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private Long id;
    private Long speakerId;
    private String speakerName;
    private Integer sequence;
    private String text;
    private String audioStatus;
    private String audioUrl;
    private Integer audioDurationMs;

    public static MessageVO fromEntity(ConversationMessage entity) {
        if (entity == null) {
            return null;
        }
        return MessageVO.builder()
                .id(entity.getId())
                .speakerId(entity.getSpeakerId())
                .speakerName(entity.getSpeakerName())
                .sequence(entity.getSequence())
                .text(entity.getText())
                .audioStatus(entity.getAudioStatus())
                .audioUrl(entity.getAudioUrl())
                .audioDurationMs(entity.getAudioDurationMs())
                .build();
    }
}
