cmake_minimum_required(VERSION 3.22.1)
project(filmvault LANGUAGES CXX)

add_library(filmvault SHARED native-lib.cpp)
target_compile_features(filmvault PRIVATE cxx_std_20)
target_link_libraries(filmvault android log)
