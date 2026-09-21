-- Allow NONE in promo_type check constraint for campaign
ALTER TABLE campaign
    DROP CONSTRAINT IF EXISTS campaign_promo_type_check;

ALTER TABLE campaign
    ADD CONSTRAINT campaign_promo_type_check CHECK (promo_type IN (
        'NONE',
        'DISCOUNT',
        'AMOUNT',
        'BOGO',
        'FREE_ITEM',
        'CUSTOM'
    ) OR promo_type IS NULL);
