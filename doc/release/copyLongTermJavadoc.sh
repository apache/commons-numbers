#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

# List of all modules paths for which the long-term Javadoc links must be copied
# We keep only the official distribution (i.e. _not_ "commons-numbers-examples").
MODULES=(commons-numbers-angle \
             commons-numbers-arrays \
             commons-numbers-combinatorics \
             commons-numbers-complex \
             commons-numbers-core \
             commons-numbers-field \
             commons-numbers-fraction \
             commons-numbers-gamma \
             commons-numbers-primes \
             commons-numbers-quaternion \
             commons-numbers-rootfinder)

while getopts r:v: option
do
    case "${option}"
    in
        r) REVISION=${OPTARG};;
        v) VERSION=${OPTARG};;
    esac
done

if [ "$REVISION" == "" ]; then
    echo "Missing SVN revision: Specify '-r <svn commit id>'";
    exit 1;
fi

if [ "$VERSION" == "" ]; then
    echo "Missing component version: Specify '-v <component version id>'";
    exit 1;
fi

for mod in ${MODULES[@]}; do
    echo $mod
    CPLIST+=" cp $REVISION $mod/apidocs $mod/javadocs/api-$VERSION"
done

echo -n "Copying long-term links ... "
svnmucc -U https://svn.apache.org/repos/infra/websites/production/commons/content/proper/commons-numbers \
        $CPLIST \
        -m "Commons Numbers: Copying $VERSION apidocs to versioned directories for the long-term links."
echo "Done."
