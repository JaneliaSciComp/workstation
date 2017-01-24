package org.janelia.jacs2.model.service;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

public class JacsServiceDataTest {

    @Test
    public void dependenciesTraversal() {
        JacsServiceData root = createJacsServiceData("n0");
        JacsServiceData n1_1 = createJacsServiceData("n1.1");
        JacsServiceData n1_2 = createJacsServiceData("n1.2");
        JacsServiceData n1_1_1 = createJacsServiceData("n1.1.1");
        JacsServiceData n1_1_2 = createJacsServiceData("n1.1.2");
        JacsServiceData n1_2_1 = createJacsServiceData("n1.2.1");

        root.addServiceDependency(n1_1);
        root.addServiceDependency(n1_2);
        n1_1.addServiceDependency(n1_2);
        n1_1.addServiceDependency(n1_1_1);
        n1_1.addServiceDependency(n1_1_2);
        n1_1_2.addServiceDependency(n1_1_1);
        n1_2.addServiceDependency(n1_2_1);

        List<JacsServiceData> rootHierarchy = root.serviceHierarchyStream().collect(Collectors.toList());
        assertThat(rootHierarchy, contains(root, n1_1, n1_2, n1_2_1, n1_1_1, n1_1_2));

        List<JacsServiceData> n1_1Hierarchy = n1_1.serviceHierarchyStream().collect(Collectors.toList());
        assertThat(n1_1Hierarchy, contains(n1_1, n1_2, n1_2_1, n1_1_1, n1_1_2));
    }

    private JacsServiceData createJacsServiceData(String name) {
        JacsServiceData sd = new JacsServiceData();
        sd.setName(name);
        return sd;
    }
}
