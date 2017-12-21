-- :name create-message! :<! :1
INSERT INTO message
(body)
VALUES (:body)
RETURNING id, body

-- :name get-message :? :1
SELECT * FROM message
WHERE id = :id::UUID

-- :name update-message! :! :n
UPDATE message
SET body = :body
WHERE id = :id::UUID

-- :name delete-message! :! :n
DELETE FROM message
WHERE id = :id::UUID

-- :name get-messages :? :*
SELECT * FROM message LIMIT :limit OFFSET :offset;
