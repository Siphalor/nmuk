#!/bin/env sh

if [ $# == 0 ] ; then
  echo "Error: must supply the command to run for every MC version"
  exit 1
fi

mcVersions=$(find ./gradle -maxdepth 1 -type d -name 'mc-*' | sed -n -e 's#.*mc-\([0-9.]\+\).*#\1#g' -e p)
echo "Discovered MC versions:
$mcVersions"

for mcVersion in $mcVersions ; do
  echo "Running \"$*\" for MC version $mcVersion"
  sed gradle.properties -i -e "s#\(minecraft\.version\.descriptor = \).*#\1$mcVersion#"
  "$@"
done
