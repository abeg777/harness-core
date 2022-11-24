#!/bin/bash
# Copyright 2022 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

FILES_WITHOUT_STMTS=()

HARNESS_INC="Copyright [0-9]{4} Harness Inc. All rights reserved."

MERGE_SUMMARY=$(git diff HEAD@{0} HEAD@{1} --name-only | grep -v '\.yml' | grep -v '\.yaml' | grep -v '\.properties' | grep -v '\.md')

echo "$MERGE_SUMMARY"

for file in ${MERGE_SUMMARY[@]}
do
    [ -z "$(grep -E "${HARNESS_INC}" $file)" ] && FILES_WITHOUT_STMTS+=( "$file" )
done

len=${#FILES_WITHOUT_STMTS[@]}

if [ $len -eq 0 ]; then
  echo "INFO: All files have copyright statement..."
else
  echo "ERROR: Following ${len} files do not have the Copyright statements. please update and re-trigger the execution..."
  for file in ${FILES_WITHOUT_STMTS[@]}
  do
    echo "-> $file"
  done
  exit 1
fi