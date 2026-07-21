package com.myreport.service;

import com.myreport.entity.GradEmploymentRecord;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradEmploymentStatFieldCatalogTest {

    @Test
    void listOptions_containsCollegeNameWithChineseLabel() {
        List<Map<String, String>> fields = GradEmploymentStatFieldCatalog.listOptions();
        assertTrue(fields.size() >= 50);

        Map<String, String> college = null;
        for (Map<String, String> f : fields) {
            if ("college_name".equals(f.get("value"))) {
                college = f;
                break;
            }
        }
        assertNotNull(college);
        assertEquals("学院名称", college.get("label"));
    }

    @Test
    void listOptions_valuesAreUnique() {
        List<Map<String, String>> fields = GradEmploymentStatFieldCatalog.listOptions();
        Set<String> values = new HashSet<String>();
        for (Map<String, String> f : fields) {
            assertTrue(values.add(f.get("value")), "duplicate value: " + f.get("value"));
        }
        assertEquals(fields.size(), values.size());
        assertEquals(GradEmploymentStatFieldCatalog.size(), fields.size());
    }

    @Test
    void canonicalize_acceptsSnakeAndCamel() {
        assertEquals("college_name", GradEmploymentStatFieldCatalog.canonicalize("college_name"));
        assertEquals("college_name", GradEmploymentStatFieldCatalog.canonicalize("collegeName"));
        assertNull(GradEmploymentStatFieldCatalog.canonicalize("not_a_real_field"));
        assertNull(GradEmploymentStatFieldCatalog.canonicalize(""));
        assertFalse(GradEmploymentStatFieldCatalog.isKnown("foo_bar"));
        assertTrue(GradEmploymentStatFieldCatalog.isKnown("destination_category"));
    }

    @Test
    void resolveExtractor_readsCollegeName() {
        Function<GradEmploymentRecord, String> fn =
                GradEmploymentStatFieldCatalog.resolveExtractor("college_name");
        assertNotNull(fn);
        GradEmploymentRecord row = new GradEmploymentRecord();
        row.setCollegeName("公路工程学院");
        assertEquals("公路工程学院", fn.apply(row));
    }

    @Test
    void labelOf_collegeName() {
        assertEquals("学院名称", GradEmploymentStatFieldCatalog.labelOf("college_name"));
        assertNull(GradEmploymentStatFieldCatalog.labelOf("unknown"));
    }
}
