java -ea -cp ./game-api/build/libs/game-api.jar:./sample/build/libs/sample.jar:./server/build/libs/server.jar:./woodsman/build/libs/woodsman.jar:./lion/build/libs/lion.jar:./sample/build/libs/sample.jar com/linkedin/domination/server/Server -p3 com.linkedin.domination.sample.Lion -p1 com.linkedin.domination.sample.WoodsmanPlayer -p2 com.linkedin.domination.sample.SamplePlayer -n 10000
