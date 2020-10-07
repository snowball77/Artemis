package de.tum.in.www1.artemis.domain.modeling;

import javax.persistence.*;

import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.enumeration.DiagramType;

/**
 * A ModelingExercise.
 */
@Entity
@DiscriminatorValue(value = "M")
public class ModelingExercise extends Exercise {

    @Enumerated(EnumType.STRING)
    @Column(name = "diagram_type")
    private DiagramType diagramType;

    @Column(name = "sample_solution_model")
    @Lob
    private String sampleSolutionModel;

    @Column(name = "sample_solution_explanation")
    @Lob
    private String sampleSolutionExplanation;

    public DiagramType getDiagramType() {
        return diagramType;
    }

    public void setDiagramType(DiagramType diagramType) {
        this.diagramType = diagramType;
    }

    public String getSampleSolutionModel() {
        return sampleSolutionModel;
    }

    public void setSampleSolutionModel(String sampleSolutionModel) {
        this.sampleSolutionModel = sampleSolutionModel;
    }

    public String getSampleSolutionExplanation() {
        return sampleSolutionExplanation;
    }

    public void setSampleSolutionExplanation(String sampleSolutionExplanation) {
        this.sampleSolutionExplanation = sampleSolutionExplanation;
    }

    /**
     * set all sensitive information to null, so no info with respect to the solution gets leaked to students through json
     */
    @Override
    public void filterSensitiveInformation() {
        setSampleSolutionModel(null);
        setSampleSolutionExplanation(null);
        super.filterSensitiveInformation();
    }

    @Override
    public String toString() {
        return "ModelingExercise{" + "id=" + getId() + ", maxScore='" + getMaxScore() + "'" + ", diagramType='" + getDiagramType() + "'" + ", sampleSolutionModel='"
                + getSampleSolutionModel() + "'" + ", sampleSolutionExplanation='" + getSampleSolutionExplanation() + "'" + "}";
    }

}
