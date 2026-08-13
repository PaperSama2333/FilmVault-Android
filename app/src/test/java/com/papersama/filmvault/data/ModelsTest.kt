package com.papersama.filmvault.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {
    @Test
    fun statusFollowsPhotographyAndLabLifecycle() {
        assertEquals(RollStatus.UNSHOT, computeStatus(0, 36, LabRecord()))
        assertEquals(RollStatus.SHOOT, computeStatus(18, 36, LabRecord()))
        assertEquals(RollStatus.WASH, computeStatus(36, 36, LabRecord()))
        assertEquals(RollStatus.SENT, computeStatus(36, 36, LabRecord(LabStatus.SENT)))
        assertEquals(RollStatus.DONE, computeStatus(36, 36, LabRecord(LabStatus.SENT, scanned = true)))
    }

    @Test
    fun customTagsAcceptChineseAndEnglishDelimiters() {
        assertEquals(listOf("旅行", "人像", "夜景"), splitTags("旅行，人像 night".replace("night", "夜景")))
    }
}
