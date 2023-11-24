cmake_minimum_required(VERSION 3.22.1)
project("ndkoneday")

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
        ./giflib/gifalloc.c
        ./giflib/dgif_lib.c
        )

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        jnigraphics)