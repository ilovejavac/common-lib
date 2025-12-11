package com.dev.lib.cache.common;

import org.redisson.api.RScoredSortedSet;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

public class CacheScoredSortedSet<T> {

    private final String              key;

    private final Duration            ttl;

    private final RScoredSortedSet<T> rScoredSortedSet;

    public CacheScoredSortedSet(String key, Duration ttl, RScoredSortedSet<T> rScoredSortedSet) {

        this.key = key;
        this.ttl = ttl;
        this.rScoredSortedSet = rScoredSortedSet;
    }

    private void expireIfNeeded() {

        if (ttl != null && rScoredSortedSet.isExists()) {
            rScoredSortedSet.expire(ttl);
        }
    }

    public boolean add(double score, T object) {

        boolean result = rScoredSortedSet.add(
                score,
                object
        );
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean addAll(Map<T, Double> objects) {

        int result = rScoredSortedSet.addAll(objects);
        if (result > 0) {
            expireIfNeeded();
        }
        return result > 0;
    }

    public Double addScore(T object, Number value) {

        Double result = rScoredSortedSet.addScore(
                object,
                value
        );
        expireIfNeeded();
        return result;
    }

    public Double getScore(T object) {

        return rScoredSortedSet.getScore(object);
    }

    public Integer rank(T object) {

        return rScoredSortedSet.rank(object);
    }

    public Integer revRank(T object) {

        return rScoredSortedSet.revRank(object);
    }

    public Collection<T> valueRange(int startIndex, int endIndex) {

        return rScoredSortedSet.valueRange(
                startIndex,
                endIndex
        );
    }

    public Collection<T> valueRangeReversed(int startIndex, int endIndex) {

        return rScoredSortedSet.valueRangeReversed(
                startIndex,
                endIndex
        );
    }

    public Collection<T> valueRange(
            double startScore,
            boolean startScoreInclusive,
            double endScore,
            boolean endScoreInclusive
    ) {

        return rScoredSortedSet.valueRange(
                startScore,
                startScoreInclusive,
                endScore,
                endScoreInclusive
        );
    }

    public Collection<T> valueRangeReversed(
            double startScore,
            boolean startScoreInclusive,
            double endScore,
            boolean endScoreInclusive
    ) {

        return rScoredSortedSet.valueRangeReversed(
                startScore,
                startScoreInclusive,
                endScore,
                endScoreInclusive
        );
    }

    public boolean remove(T object) {

        boolean result = rScoredSortedSet.remove(object);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public int removeRangeByRank(int startIndex, int endIndex) {

        int result = rScoredSortedSet.removeRangeByRank(
                startIndex,
                endIndex
        );
        if (result > 0) {
            expireIfNeeded();
        }
        return result;
    }

    public int removeRangeByScore(
            double startScore,
            boolean startScoreInclusive,
            double endScore,
            boolean endScoreInclusive
    ) {

        int result = rScoredSortedSet.removeRangeByScore(
                startScore,
                startScoreInclusive,
                endScore,
                endScoreInclusive
        );
        if (result > 0) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean contains(Object o) {

        return rScoredSortedSet.contains(o);
    }

    public int size() {

        return rScoredSortedSet.size();
    }

    public boolean isEmpty() {

        return rScoredSortedSet.isEmpty();
    }

    public void clear() {

        rScoredSortedSet.clear();
    }

    public Collection<T> readAll() {

        return rScoredSortedSet.readAll();
    }

    public RScoredSortedSet<T> raw() {

        return rScoredSortedSet;
    }

}