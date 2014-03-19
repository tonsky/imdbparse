# imdbparse

A project that converts IMDb text database into regular machine-readable format (currently EDN).

## Usage

Please refer to [imdb.com/interfaces](http://www.imdb.com/interfaces) on how to download IMDb text database.

Download and unzip IMDB archives:

    imdb/
      actors.list
      cinematographers.list
      genres.list
      actresses.list
      composers.list
      directors.list
      movies.list
      ratings.list
      writers.list

Currently, only files listed above are supported.

Run:

    lein run <path/to/imdb/>

You should see something like:

    [ CONVERTING ] imdb/movies.list to imdb/movies.list.edn .......... DONE
    [ CONVERTING ] imdb/ratings.list to imdb/ratings.list.edn .... DONE
    [ CONVERTING ] imdb/genres.list to imdb/genres.list.edn ............... DONE

## License

Copyright © 2014 Nikita Prokopov

Distributed under the The MIT License.
