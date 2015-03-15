The following code can be found on pug under ~/Netflix/pyflix-0.1/pyflix/algorithms/svd/demo.py

```
import sys
from itertools import izip
import loadNetflix

# grid is the name of the directory which holds the counts
counts = loadNetflix.load_counts('grid')

# load userHash and movieHash
userHash, movieHash = loadNetflix.load_data()

# load just userHash
# userHash, _ = loadNetflix.load_data(do_movies = False)

# load just movieHash
# _, movieHash = loadNetflix.load_data(do_users = False)

# show the number of movies in common between movie ids 100 and 200
print counts(100, 200)

# show all of the movie, rating pairs for a given user:
user_id = 6
for movie_id, rating in izip(userHash[user_id][0], userHash[user_id][1]) :
        print movie_id, rating


# show all of the user, rating pairs for a given movie:
movie_id = 5
for user_id, rating in izip(movieHash[movie_id][0], movieHash[movie_id][1]) :
        print user_id, rating

# loop from all of the movies and find its average rating:
for movie, ratings in movieHash.iteritems() :
        print movie, float(sum(ratings[1]))/len(ratings[1])


```