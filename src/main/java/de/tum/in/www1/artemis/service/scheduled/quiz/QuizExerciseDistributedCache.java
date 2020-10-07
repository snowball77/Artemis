package de.tum.in.www1.artemis.service.scheduled.quiz;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.internal.serialization.impl.SerializationServiceV1;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.scheduledexecutor.ScheduledTaskHandler;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.domain.quiz.QuizSubmission;

/**
 * This class represents the cache for a single quiz exercise.
 * <p>
 * This includes the participations, submissions and results, but also the quiz exercise object itself and handlers for
 * the distributed start task {@link QuizStartTask}.
 */
final class QuizExerciseDistributedCache extends QuizExerciseCache implements HazelcastInstanceAware {

    private static final Logger log = LoggerFactory.getLogger(QuizExerciseDistributedCache.class);

    private static final String HAZELCAST_CACHE_PARTICIPATIONS = "-participations";

    private static final String HAZELCAST_CACHE_SUBMISSIONS = "-submissions";

    private static final String HAZELCAST_CACHE_RESULTS = "-results";

    /**
     * All {@link List} classes that are supported by Hazelcast {@link SerializationServiceV1}
     */
    private static final Set<Class<?>> SUPPORTED_LIST_CLASSES = Set.of(ArrayList.class, LinkedList.class, CopyOnWriteArrayList.class);

    /**
     * Make sure this is a class of SUPPORTED_LIST_CLASSES to make easy serialization possible, see {@link SerializationServiceV1}
     */
    List<ScheduledTaskHandler> quizStart;

    /**
     * The {@link QuizExercise} cached object. This is only cached locally and not distributed.
     */
    private transient QuizExercise exercise;

    /*
     * All three IMaps are distributed Hazelcast objects and must not be (de-)serialized, they are all set in the setHazelcastInstance method.
     */

    private transient IMap<String, StudentParticipation> participations;

    private transient IMap<String, QuizSubmission> submissions;

    /**
     * Must be a Map because Hazelcast uses serialized objects for set operations and not hashCode()/equals()
     */
    private transient IMap<Long, Result> results;

    QuizExerciseDistributedCache(Long exerciseId, List<ScheduledTaskHandler> quizStart, QuizExercise exercise) {
        super(Objects.requireNonNull(exerciseId, "exerciseId must not be null"));
        setQuizStart(quizStart);
        setExercise(exercise);
        log.debug("Creating new QuizExerciseDistributedCache, id {}", getExerciseId());
    }

    QuizExerciseDistributedCache(Long exerciseId, List<ScheduledTaskHandler> quizStart) {
        this(exerciseId, quizStart, null);
    }

    QuizExerciseDistributedCache(Long exerciseId) {
        this(exerciseId, getEmptyQuizStartList());
    }

    @Override
    QuizExercise getExercise() {
        return exercise;
    }

    @Override
    Map<String, QuizSubmission> getSubmissions() {
        return submissions;
    }

    @Override
    Map<String, StudentParticipation> getParticipations() {
        return participations;
    }

    @Override
    List<ScheduledTaskHandler> getQuizStart() {
        return quizStart;
    }

    @Override
    Map<Long, Result> getResults() {
        return results;
    }

    @Override
    void setExercise(QuizExercise newExercise) {
        this.exercise = newExercise;
    }

    @Override
    void setQuizStart(List<ScheduledTaskHandler> quizStart) {
        Objects.requireNonNull(quizStart);
        if (SUPPORTED_LIST_CLASSES.contains(quizStart.getClass())) {
            this.quizStart = quizStart;
        }
        else {
            this.quizStart = new ArrayList<>(quizStart);
        }
    }

    @Override
    void clear() {
        int participationsSize = participations.size();
        int submissionsSize = submissions.size();
        int resultsSize = results.size();
        if (participationsSize > 0) {
            log.warn("Cache for Quiz {} destroyed with {} participations cached", getExerciseId(), participationsSize);
        }
        if (submissionsSize > 0) {
            log.warn("Cache for Quiz {} destroyed with {} submissions cached", getExerciseId(), submissionsSize);
        }
        if (resultsSize > 0) {
            log.warn("Cache for Quiz {} destroyed with {} results cached", getExerciseId(), resultsSize);
        }
        participations.destroy();
        submissions.destroy();
        results.destroy();
        exercise = null;
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        /*
         * Distributed Hazelcast objects will be automatically created and set up by Hazelcast, and are cached by the Hazelcast instance itself globally. This is a relatively
         * lightweight operation.
         */
        participations = hazelcastInstance.getMap(Constants.HAZELCAST_QUIZ_PREFIX + getExerciseId() + HAZELCAST_CACHE_PARTICIPATIONS);
        submissions = hazelcastInstance.getMap(Constants.HAZELCAST_QUIZ_PREFIX + getExerciseId() + HAZELCAST_CACHE_SUBMISSIONS);
        results = hazelcastInstance.getMap(Constants.HAZELCAST_QUIZ_PREFIX + getExerciseId() + HAZELCAST_CACHE_RESULTS);
    }

    /**
     * A serializer and deserializer for distributed quiz cache objects, required for objects distributed via Hazelcast.
     * We cannot use standard Java-serialization here, because the individual fields of {@link QuizExerciseDistributedCache}
     * need to use different serialization mechanisms (e.g. {@link ScheduledTaskHandler} is not {@link Serializable}).
     */
    static class QuizExerciseDistributedCacheStreamSerializer implements StreamSerializer<QuizExerciseDistributedCache> {

        @Override
        public int getTypeId() {
            return Constants.HAZELCAST_QUIZ_EXERCISE_CACHE_SERIALIZER_ID;
        }

        @Override
        public void write(ObjectDataOutput out, QuizExerciseDistributedCache exerciseCacheImpl) throws IOException {
            // Hazelcast will choose the best fit from it's own serializers for each object
            out.writeLong(exerciseCacheImpl.getExerciseId());
            out.writeObject(exerciseCacheImpl.quizStart);
            out.writeObject(exerciseCacheImpl.exercise);
        }

        @Override
        public QuizExerciseDistributedCache read(ObjectDataInput in) throws IOException {
            Long exerciseId = in.readLong();
            List<ScheduledTaskHandler> quizStart = in.readObject();
            QuizExercise exercise = in.readObject();
            return new QuizExerciseDistributedCache(exerciseId, quizStart, exercise);
        }
    }

    static void registerSerializer(Config config) {
        SerializerConfig serializerConfig = new SerializerConfig();
        serializerConfig.setTypeClass(QuizExerciseDistributedCache.class);
        serializerConfig.setImplementation(new QuizExerciseDistributedCacheStreamSerializer());
        config.getSerializationConfig().addSerializerConfig(serializerConfig);
    }
}
