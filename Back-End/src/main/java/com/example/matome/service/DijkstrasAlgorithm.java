package com.example.matome.service;


import com.example.matome.domain.Path;
import com.example.matome.domain.PlanetName;
import com.example.matome.dto.GetShortestRouteRequest;
import com.example.matome.dto.GetShortestRouteResponses;
import com.example.matome.repository.PlanetNameRepository;
import com.example.matome.repository.pathRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The algorithm used is generic and widely know and what is makes it interesting in this instance is how it is applied
 * to persist the data the graph.
 */

@Component
public class DijkstrasAlgorithm {

    Logger logger = LoggerFactory.getLogger(DijkstrasAlgorithm.class);


    //The graph contain the weight between nodes.
    private Double graph[][];

    private Double[] shortestDistances;

    boolean[] added;

    private int[] parents;

    private List<String> desiredShortestPath = new ArrayList<>();

    private static final int NO_PARENT = -1;


    @Autowired
    pathRepository pathRepository;

    @Autowired
    PlanetNameRepository planetNameRepository;


    /**
     * We initialise the graph and build it as the adjacency matrix
     * We initialise all rows and columns to contains 0s initiali
     * Then the relevant indices are populated
     * So the worst case time complexity  O(n^2)
     */


    private void init(boolean withDelay) {
        desiredShortestPath = new ArrayList<>();
        List<PlanetName> planetNameList = planetNameRepository.findAll();
        int dimensions = planetNameList.size() + 1;

        graph = new Double[dimensions][dimensions];

        shortestDistances = new Double[dimensions];

        added = new boolean[dimensions];

        parents = new int[dimensions];

        for (int itr = 0; itr < dimensions; itr++) {
            for (int itc = 0; itc < dimensions; itc++) {
                graph[itr][itc] = 0.0;

            }
        }

        /**
         *  Build the matrix of doubles because our distance is constructed as double
         */


        List<Path> paths = pathRepository.findAll();
        int size = paths.size();

        paths.forEach(path -> {
            Double distance = path.getDistance();
            Double trafficDelay = path.getTrafficDelay();

            /**
             * This build undirected graph.
             * The assumption is that the graph distance between node in longer when there is
             * traffic delay.
             */
            if (withDelay) {
                graph[path.getOrigin()][path.getDestination()] = distance + trafficDelay;
                graph[path.getDestination()][path.getOrigin()] = distance + trafficDelay;
            } else {
                graph[path.getDestination()][path.getOrigin()] = distance;
                graph[path.getOrigin()][path.getDestination()] = distance;
            }

        });


        /**
         * Initialise the shortest distances and added
         */

        for (int itr = 0; itr < dimensions; itr++) {
            shortestDistances[itr] = Double.MAX_VALUE;
            added[itr] = false;
        }

        logger.info("Finished initialisation");

    }


    private void dijkstra(Integer origin) {

        shortestDistances[origin] = 0.0;

        parents[origin] = NO_PARENT;


        int vertices = graph[0].length;

        for (int itr = 1; itr < vertices; itr++) {

            int nearestVertex = -1;
            Double shortestDistance = Double.MAX_VALUE;

            for (int itr2 = 0; itr2 < vertices; itr2++) {
                if (!added[itr2] && shortestDistances[itr2] < shortestDistance) {
                    nearestVertex = itr2;
                    shortestDistance = shortestDistances[itr2];
                }
            }

            //Keep track of all nodes which have been visited and added to the shortest path tree.

            added[nearestVertex] = true;

            for (int itr2 = 0; itr2 < vertices; itr2++) {
                Double edgeDistance = graph[nearestVertex][itr2];

                if (edgeDistance > 0
                        && ((shortestDistance + edgeDistance) <
                        shortestDistances[itr2])) {
                    parents[itr2] = nearestVertex;
                    ///Update the distance from one point to another
                    shortestDistances[itr2] = shortestDistance +
                            edgeDistance;
                }
            }

        }

    }

    /**
     * Recursive method to compute the shortes distance path
     *
     * @param vertex
     * @param parents
     */
    private void computePath(Integer vertex, int[] parents) {
        if (vertex == NO_PARENT) {
            return;
        }
        computePath(parents[vertex], parents);
        PlanetName planetName = planetNameRepository.findByIndex(vertex);
        desiredShortestPath.add(planetName.getPlanetName());
    }


    /**
     * Utility function to aggregate the methods and compute the results
     *
     * @param req
     * @return
     */
    public GetShortestRouteResponses findShortestPath(GetShortestRouteRequest req) {
        PlanetName ori = planetNameRepository.findByPlanetName(req.getOrigin());
        PlanetName des = planetNameRepository.findByPlanetName(req.getDestination());

        if (Objects.isNull(ori) || Objects.isNull(des)) {
            return null;
        } else {
            GetShortestRouteResponses res = new GetShortestRouteResponses();
            init(req.getTrafficInfo());
            dijkstra(ori.getIndex());
            computePath(des.getIndex(), parents);
            res.setOrigin(ori.getPlanetName());
            res.setDestination(des.getPlanetName());
            res.setDistance(String.valueOf(shortestDistances[des.getIndex()]));
            res.setPath(desiredShortestPath);
            return res;

        }
    }
}
