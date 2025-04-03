package me.fengorz.kiwi.ai.api.vo.ytb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YtbChannelVO implements Serializable {


    private static final long serialVersionUID = 3492807617265284485L;

    private String channelName;

    private Long channelId;

    private Integer status;

}
