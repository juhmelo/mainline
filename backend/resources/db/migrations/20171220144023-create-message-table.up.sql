CREATE TABLE ml.message
(id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
body TEXT NOT NULL);
