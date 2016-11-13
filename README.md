# kmeans
KMeans Implementation.....In Parallel!!!


Steps allows to run a fixed number of steps. After this, the k-means algorithm is stopped.
Eta corresponds to the means stability, as we showed earlier: if the means did not move much since the last iteration, the result is considered stable.
Sound-to-noise ratio is a more refined convergence strategy, which does not settle for stability but tries to minimize the difference between the true color image and the index color one. 
This strategy goes beyond Eta, but high Sound-to-noise ratios will prevent the k-means algorithm from finishing!

At each iteration of K-means, we can associate multiple points to clusters, and compute the average of the k clusters, in parallel.
Note that the association of a point to its cluster is independent of the other points in the input, and similarly, the computation of the
average of a cluster is independent of the other clusters. Once all parallel tasks of the current iteration complete, the algorithm can proceed
to the next iteration.

K-means a bulk synchronous parallel algorithm (BSP). BSP algorithms are composed from a sequence of supersteps, each of which contains:
parallel computation, in which processes independently perform local computations and produce some values communication, in 
which processes exchange data barrier synchronisation, during which processes wait until every process finishes
Data-parallel programming models are typically a good fit for BSP algorithms, as each bulk synchronous phase can correspond to
some number of data-parallel operations.

The k-means algorithm is very sensitive to the initial choice of means. There are three choice strategies implemented in ScalaShop:

Uniform Choice is the simplest strategy. It chooses n colors uniformly in the entire color space, regardless of the colors used in the image.
If the image has a dominant color, the means created by this strategy will likely be very far away from the clusters formed by this dominant color.
You can try setting the Uniform Choice strategy with 1, 10 and 30 steps. You will notice the initial choice is quite bad, but the quality improves as
the k-means algorithm is applied in more steps.

Random Sampling is another simple strategy, but with better results. For the initial means, it randomly samples n colors from the image.
This yields good results if the image has few dominant colors, but it cannot handle subtle nuances in the image. Again, if you try this strategy
with 1, 10 and 30 k-means iteration steps, you will notice improvements as the k-means algorithm is ran more.

Uniform Random is the most complex strategy to pick means, but it also produces the best results. It works by uniformly splitting the color space in sub-spaces.
It then counts the number of pixels that have colors belonging to that sub-space. Based on this number, it chooses a proportional number of means in the
sub-space, by randomly sampling from the pixels in that sub-space. Therefore, if your image has dominant colors, this strategy will drop a proportional number o
f means for each dominant color, thus allowing the k-means algorithm to capture fine nuances.

