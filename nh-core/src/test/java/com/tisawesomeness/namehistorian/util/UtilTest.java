package com.tisawesomeness.namehistorian.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilTest {

    @Test
    public void testMapNullable() {
        assertThat(Util.mapNullable(null, String::length)).isNull();
    }
    @Test
    public void testMapNullable2() {
        assertThat(Util.mapNullable("abc", String::length)).isEqualTo(3);
    }

    @ParameterizedTest(name = "{index} ==> String `{0}` is blank")
    @ValueSource(strings = {"", " ", "  ", "\n"})
    public void testBlank(String str) {
        assertThat(Util.isBlank(str)).isTrue();
    }
    @Test
    public void testNotBlank() {
        assertThat(Util.isBlank("a")).isFalse();
    }

    @ParameterizedTest(name = "{index} ==> Short string {0} maps to a UUID")
    @ValueSource(strings = {
            "11a5fc8d671b437d94c016b9e1cac57f",
            "7f516da36c034bd4885836e24c7b740e",
            "f7e9d6a2a55d492d8a8827eb37dd9781",
            "09177efe45994f48938623ffe9c10a59",
            "61be5ed6bc0e48a59e4c5c98188a020d",
            "0ec4c9cF44b04c748511be3eac31b622", // mixed case
            "7535348FD7F44A63A738FBE75E0BAEDD",
            "BE483D833CBF48208DE668EE1887A327"
    })
    @DisplayName("Short strings are correctly mapped to a UUID")
    public void testFromStringShort(String candidate) {
        assertThat(Util.parseUUID(candidate))
                .isPresent()
                .map(u -> u.toString().replace("-", ""))
                .get().asString()
                .isEqualToIgnoringCase(candidate);
    }

    @ParameterizedTest(name = "{index} ==> Long string {0} maps to a UUID")
    @ValueSource(strings = {
            "b82d4448-11ba-4da3-8e2f-e010309fcb95",
            "50c3ed25-ed57-4b66-a95a-b6ff67b923cb",
            "7adc1910-2d7b-4893-9119-7022e15f2028",
            "a8dc21ed-5fbe-491b-8338-6e0d390762f7",
            "af94c224-8cd3-4380-ae80-796a94a863bc",
            "cc739741-baE6-4286-98eb-1f45c4116cf9", // mixed case
            "8FF3E250-78CC-4609-87EC-B601AF05FB64",
            "5D351014-AAF9-409D-A608-6F996D0BCF34"
    })
    @DisplayName("Long strings are correctly mapped to a UUID")
    public void testFromStringLong(String candidate) {
        assertThat(Util.parseUUID(candidate))
                .isPresent()
                .map(UUID::toString)
                .get().asString()
                .isEqualToIgnoringCase(candidate);
    }

    @ParameterizedTest(name = "{index} ==> String `{0}` is an invalid UUID")
    @EmptySource
    @ValueSource(strings = {
            " ", "   ", "\n", "\t", // Whitespace
            "Tis_awesomeness", // Username
            "jeb_",
            "81c978f3-7973-44cf-a1ac-664b329cf0e", // 31 chars
            "6d18209f89c743bba06771cb3e2cf89",
            "045d8f0a-38c9-41ff-99c2-9a01dee954803", // 33 chars
            "38eb7e2de0294273bccd18be389bf6db3",
            "2d9ab0dg-a2d5-4a73-a5a0-27e139cfcc6e", // Invalid character
            "9cc0c774-9494-4a98=b138-f9885b9b1f74",
            "a957-a311-7f15-4aa9-b9cd-39c6-7952-2b84", // Too many dashes
            "aed0c3d0e4e0-4361-930c-1c968f5c1189", // Too few dashes
            "02264bd9--8a60-46e7-950d-9ee6c3a66017" // Double dash
    })
    @DisplayName("Using parseUUID() on an invalid UUID string returns empty")
    public void testFromStringInvalid(String candidate) {
        assertThat(Util.parseUUID(candidate)).isEmpty();
    }

}
