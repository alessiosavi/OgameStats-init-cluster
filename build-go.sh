#!/bin/bash

rm bin/*.zip

folder=$(pwd)
# Build and zip the GoStatOgame
cd "$folder"/../GoStatOgame/ || exit
go build
strip -s GoStatOgame
zip GoStatOgame.zip GoStatOgame
cp GoStatOgame.zip "$folder"/bin

# Build and zip the PopulatePlayerDB
cd "$folder"/../PopulatePlayerDB/ || exit
go build
strip -s PopulatePlayerDB
zip PopulatePlayerDB.zip PopulatePlayerDB
cp PopulatePlayerDB.zip "$folder"/bin

cd "$folder" || exit
