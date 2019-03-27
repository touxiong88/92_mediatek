#!/bin/sh
if [ -z $1 ]; then
    echo "Usage: install.sh \$(gettop)"
else
    T=$1
    cd $T/packages/apps/Launcher2
    ln -s $T/mediatek/source/packages/Media3D/patches-Launcher2 patches
    quilt push -a
fi
