package com.example.worker.domain.song.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SongFileNamePolicyTest {

    @Test
    void sanitize_null이면_알수없음() {
        assertThat(SongFileNamePolicy.sanitize(null)).isEqualTo("알수없음");
    }

    @Test
    void sanitize_특수문자_공백_제거() {
        String input = "a b_c-d|e?f*\"< > / \\ :";
        String out = SongFileNamePolicy.sanitize(input);

        assertThat(out).doesNotContain(" ");
        assertThat(out).doesNotContain("_");
        assertThat(out).doesNotContain("-");
        assertThat(out).doesNotContain("|");
        assertThat(out).doesNotContain("?");
        assertThat(out).doesNotContain("*");
        assertThat(out).doesNotContain("\"");
        assertThat(out).doesNotContain("<");
        assertThat(out).doesNotContain(">");
        assertThat(out).doesNotContain("/");
        assertThat(out).doesNotContain("\\");
        assertThat(out).doesNotContain(":");
    }
}
