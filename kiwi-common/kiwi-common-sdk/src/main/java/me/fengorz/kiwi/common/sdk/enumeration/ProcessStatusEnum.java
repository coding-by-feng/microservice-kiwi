package me.fengorz.kiwi.common.sdk.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum for processing status values
 */
@Getter
@RequiredArgsConstructor
public enum ProcessStatusEnum {
    
    /**
     * Initial status, ready to be processed
     */
    READY(0, "Ready"),
    
    /**
     * Currently being processed
     */
    PROCESSING(1, "Processing"),
    
    /**
     * Processing completed
     */
    FINISH(2, "Finish");
    
    /**
     * Status code value stored in database
     */
    private final Integer code;
    
    /**
     * Description of the status
     */
    private final String description;
    
    /**
     * Get enum by code
     * 
     * @param code The status code
     * @return The corresponding enum or null if not found
     */
    public static ProcessStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (ProcessStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        
        return null;
    }

}