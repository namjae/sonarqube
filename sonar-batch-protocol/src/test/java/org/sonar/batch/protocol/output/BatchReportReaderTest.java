/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.protocol.output;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportReaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File dir;

  BatchReportReader sut;

  @Before
  public void setUp() throws Exception {
    dir = temp.newFolder();
    sut = new BatchReportReader(dir);
  }

  @Test
  public void create_dir_if_does_not_exist() throws Exception {
    File dir = temp.newFolder();

    initFiles(dir);

    sut = new BatchReportReader(dir);
    BatchReport.Metadata readMetadata = sut.readMetadata();
    assertThat(readMetadata.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(readMetadata.getDeletedComponentsCount()).isEqualTo(1);
    assertThat(sut.readComponentIssues(1)).hasSize(1);
    assertThat(sut.readComponentIssues(200)).isEmpty();
    assertThat(sut.readComponent(1).getUuid()).isEqualTo("UUID_A");
    BatchReport.Issues deletedComponentIssues = sut.readDeletedComponentIssues(1);
    assertThat(deletedComponentIssues.getComponentUuid()).isEqualTo("compUuid");
    assertThat(deletedComponentIssues.getIssueList()).hasSize(1);
    assertThat(sut.readComponentMeasures(1)).hasSize(1);
    assertThat(sut.readComponentMeasures(1).get(0).getStringValue()).isEqualTo("value_a");
    assertThat(sut.readComponentScm(1).getChangesetList()).hasSize(1);
    assertThat(sut.readComponentScm(1).getChangeset(0).getDate()).isEqualTo(123_456_789L);
  }

  @Test
  public void read_duplications() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1).build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileKey("COMPONENT_A")
        .setOtherFileRef(2)
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(1, Arrays.asList(duplication));

    BatchReportReader sut = new BatchReportReader(dir);
    assertThat(sut.readComponentDuplications(1)).hasSize(1);
    assertThat(sut.readComponentDuplications(1).get(0).getOriginPosition()).isNotNull();
    assertThat(sut.readComponentDuplications(1).get(0).getDuplicateList()).hasSize(1);
  }

  @Test
  public void read_syntax_highlighting() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1).build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    BatchReport.SyntaxHighlighting.HighlightingRule highlightingRule = BatchReport.SyntaxHighlighting.HighlightingRule.newBuilder()
      .setRange(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setEndLine(1)
        .build())
      .setType(Constants.HighlightingType.ANNOTATION)
      .build();
    writer.writeComponentSyntaxHighlighting(1, Arrays.asList(highlightingRule));

    BatchReportReader sut = new BatchReportReader(dir);
    assertThat(sut.readComponentSyntaxHighlighting(1)).hasSize(1);
    assertThat(sut.readComponentSyntaxHighlighting(1).get(0).getRange()).isNotNull();
    assertThat(sut.readComponentSyntaxHighlighting(1).get(0).getType()).isEqualTo(Constants.HighlightingType.ANNOTATION);
  }

  @Test
  public void read_symbols() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentSymbols(1, Arrays.asList(BatchReport.Symbols.Symbol.newBuilder()
      .setDeclaration(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setStartOffset(3)
        .setEndLine(1)
        .setEndOffset(5)
        .build())
      .addReference(BatchReport.Range.newBuilder()
        .setStartLine(10)
        .setStartOffset(15)
        .setEndLine(11)
        .setEndOffset(2)
        .build())
      .build()));

    sut = new BatchReportReader(dir);
    assertThat(sut.readComponentSymbols(1)).hasSize(1);
    assertThat(sut.readComponentSymbols(1).get(0).getDeclaration().getStartLine()).isEqualTo(1);
    assertThat(sut.readComponentSymbols(1).get(0).getReference(0).getStartLine()).isEqualTo(10);
  }

  @Test
  public void read_coverage() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeFileCoverage(1, Arrays.asList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .setConditions(1)
        .setUtHits(true)
        .setItHits(false)
        .setUtCoveredConditions(1)
        .setItCoveredConditions(1)
        .setOverallCoveredConditions(1)
        .build(),
      BatchReport.Coverage.newBuilder()
        .setLine(2)
        .setConditions(5)
        .setUtHits(false)
        .setItHits(false)
        .setUtCoveredConditions(4)
        .setItCoveredConditions(5)
        .setOverallCoveredConditions(5)
        .build()));

    sut = new BatchReportReader(dir);
    List<BatchReport.Coverage> coverageList = newArrayList(sut.readFileCoverage(1));
    assertThat(coverageList).hasSize(2);

    BatchReport.Coverage coverage = coverageList.get(0);
    assertThat(coverage.getLine()).isEqualTo(1);
    assertThat(coverage.getConditions()).isEqualTo(1);
    assertThat(coverage.getUtHits()).isTrue();
    assertThat(coverage.getItHits()).isFalse();
    assertThat(coverage.getUtCoveredConditions()).isEqualTo(1);
    assertThat(coverage.getItCoveredConditions()).isEqualTo(1);
    assertThat(coverage.getOverallCoveredConditions()).isEqualTo(1);

    coverage = coverageList.get(1);
    assertThat(coverage.getLine()).isEqualTo(2);
    assertThat(coverage.getConditions()).isEqualTo(5);
    assertThat(coverage.getUtHits()).isFalse();
    assertThat(coverage.getItHits()).isFalse();
    assertThat(coverage.getUtCoveredConditions()).isEqualTo(4);
    assertThat(coverage.getItCoveredConditions()).isEqualTo(5);
    assertThat(coverage.getOverallCoveredConditions()).isEqualTo(5);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_metadata_file() throws Exception {
    sut.readMetadata();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_deleted_component() throws Exception {
    sut.readDeletedComponentIssues(666);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_component() throws Exception {
    sut.readComponent(666);
  }

  @Test
  public void empty_list_if_no_measure_found() throws Exception {
    assertThat(sut.readComponentMeasures(666)).isEmpty();
  }

  @Test
  public void null_if_no_scm_found() throws Exception {
    assertThat(sut.readComponentScm(666)).isNull();
  }

  @Test
  public void empty_list_if_no_duplication_found() throws Exception {
    assertThat(sut.readComponentDuplications(123)).isEmpty();
  }

  @Test
  public void empty_list_if_no_symbol_found() throws Exception {
    assertThat(sut.readComponentSymbols(123)).isEmpty();
  }

  @Test
  public void empty_list_if_no_highlighting_found() throws Exception {
    assertThat(sut.readComponentSyntaxHighlighting(123)).isEmpty();
  }

  @Test
  public void return_null_if_no_coverage_found() throws Exception {
    assertThat(sut.readFileCoverage(123)).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_no_source_found() throws Exception {
    assertThat(sut.readSourceLines(123)).isNull();
  }

  /**
   * no file if no issues
   */
  @Test
  public void empty_list_if_no_issue_found() throws Exception {
    assertThat(sut.readComponentIssues(666)).isEmpty();
  }

  private void initFiles(File dir) {
    BatchReportWriter writer = new BatchReportWriter(dir);

    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1)
      .setDeletedComponentsCount(1);
    writer.writeMetadata(metadata.build());

    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setUuid("UUID_A");
    writer.writeComponent(component.build());

    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .build();

    writer.writeComponentIssues(1, Arrays.asList(issue));

    writer.writeDeletedComponentIssues(1, "compUuid", Arrays.asList(issue));

    BatchReport.Measure.Builder measure = BatchReport.Measure.newBuilder()
      .setStringValue("value_a");
    writer.writeComponentMeasures(1, Arrays.asList(measure.build()));

    BatchReport.Scm.Builder scm = BatchReport.Scm.newBuilder()
      .setComponentRef(1)
      .addChangeset(BatchReport.Scm.Changeset.newBuilder().setDate(123_456_789).setAuthor("jack.daniels").setRevision("123-456-789"));
    writer.writeComponentScm(scm.build());
  }

}
