#!/bin/sh
curl localhost:8000/api -X POST -d @InvokeRequire.bin | hd