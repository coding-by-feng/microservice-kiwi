package me.fengorz.kiwi.common.ytb;

import java.io.Serializable;

public class YtbSubtitlesResult implements Serializable {

    private static final long serialVersionUID = 4200784339674285787L;

    private String videoUrl;
    private SubtitleTypeEnum type;
    private String scrollingSubtitles;
    private Object pendingToBeTranslatedOrRetouchedSubtitles;
    private String langCode; // e.g. en, en-US, zh-CN, zh-Hans

    public YtbSubtitlesResult(String videoUrl, SubtitleTypeEnum type, String scrollingSubtitles,
            Object pendingToBeTranslatedOrRetouchedSubtitles, String langCode) {
        this.videoUrl = videoUrl;
        this.type = type;
        this.scrollingSubtitles = scrollingSubtitles;
        this.pendingToBeTranslatedOrRetouchedSubtitles = pendingToBeTranslatedOrRetouchedSubtitles;
        this.langCode = langCode;
    }

    public static YtbSubtitlesResultBuilder builder() {
        return new YtbSubtitlesResultBuilder();
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public SubtitleTypeEnum getType() {
        return type;
    }

    public void setType(SubtitleTypeEnum type) {
        this.type = type;
    }

    public String getScrollingSubtitles() {
        return scrollingSubtitles;
    }

    public void setScrollingSubtitles(String scrollingSubtitles) {
        this.scrollingSubtitles = scrollingSubtitles;
    }

    public Object getPendingToBeTranslatedOrRetouchedSubtitles() {
        return pendingToBeTranslatedOrRetouchedSubtitles;
    }

    public void setPendingToBeTranslatedOrRetouchedSubtitles(Object pendingToBeTranslatedOrRetouchedSubtitles) {
        this.pendingToBeTranslatedOrRetouchedSubtitles = pendingToBeTranslatedOrRetouchedSubtitles;
    }

    public String getLangCode() {
        return langCode;
    }

    public void setLangCode(String langCode) {
        this.langCode = langCode;
    }

    @Override
    public String toString() {
        return "YtbSubtitlesResult{" +
                "videoUrl='" + videoUrl + '\'' +
                ", type=" + type +
                ", scrollingSubtitles='" + scrollingSubtitles + '\'' +
                ", pendingToBeTranslatedOrRetouchedSubtitles=" + pendingToBeTranslatedOrRetouchedSubtitles +
                ", langCode='" + langCode + '\'' +
                '}';
    }

    public static class YtbSubtitlesResultBuilder {
        private String videoUrl;
        private SubtitleTypeEnum type;
        private String scrollingSubtitles;
        private Object pendingToBeTranslatedOrRetouchedSubtitles;
        private String langCode;

        YtbSubtitlesResultBuilder() {
        }

        public YtbSubtitlesResultBuilder videoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }

        public YtbSubtitlesResultBuilder type(SubtitleTypeEnum type) {
            this.type = type;
            return this;
        }

        public YtbSubtitlesResultBuilder scrollingSubtitles(String scrollingSubtitles) {
            this.scrollingSubtitles = scrollingSubtitles;
            return this;
        }

        public YtbSubtitlesResultBuilder pendingToBeTranslatedOrRetouchedSubtitles(
                Object pendingToBeTranslatedOrRetouchedSubtitles) {
            this.pendingToBeTranslatedOrRetouchedSubtitles = pendingToBeTranslatedOrRetouchedSubtitles;
            return this;
        }

        public YtbSubtitlesResultBuilder langCode(String langCode) {
            this.langCode = langCode;
            return this;
        }

        public YtbSubtitlesResult build() {
            return new YtbSubtitlesResult(videoUrl, type, scrollingSubtitles, pendingToBeTranslatedOrRetouchedSubtitles,
                    langCode);
        }

        public String toString() {
            return "YtbSubtitlesResult.YtbSubtitlesResultBuilder(videoUrl=" + this.videoUrl + ", type=" + this.type
                    + ", scrollingSubtitles=" + this.scrollingSubtitles + ", pendingToBeTranslatedOrRetouchedSubtitles="
                    + this.pendingToBeTranslatedOrRetouchedSubtitles + ", langCode=" + this.langCode + ")";
        }
    }
}
